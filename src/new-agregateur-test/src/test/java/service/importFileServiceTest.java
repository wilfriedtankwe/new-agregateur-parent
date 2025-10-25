package service;


import com.dimsoft.agregateur.new_agregateur_prod.services.impl.ImportFileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


public class importFileServiceTest {


        @Mock

        private JdbcTemplate jdbcTemplate;

        @InjectMocks
        private ImportFileServiceImpl importFileService;

        @TempDir
        Path tempDir;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.openMocks(this);
        }



    //  Tests pour extractNumbersFromFiles

        @Test
        void testExtractNumbersFromFiles_WithCAFile() throws IOException {
            // Créer un fichier CA de test
            File caFile = tempDir.resolve("CA_test.csv").toFile();
            try (FileWriter writer = new FileWriter(caFile)) {
                writer.write("Header line\n");
                writer.write("Line 2\n");
                writer.write("Line 3\n");
                writer.write("Line 4\n");
                writer.write("Line 5\n");
                writer.write("Compte: 123456789012\n"); // Ligne 6
            }

            // Mock de la recherche en base
            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(Collections.singletonList("BANK123"));

            // Note: Ce test nécessiterait de modifier la classe pour injecter le répertoire
            // Pour un vrai test, vous devriez utiliser une approche différente
        }

        @Test
        void testExtractNumbersFromFiles_WithTCpteFile() throws IOException {
            File tcpteFile = tempDir.resolve("T_cpte_test.csv").toFile();
            try (FileWriter writer = new FileWriter(tcpteFile)) {
                writer.write("Col1;Col2;Col3;Col4\n"); // Ligne 1
                writer.write("Data1;Data2;Data3;98765\n"); // Ligne 2, colonne 4
            }

            when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                    .thenReturn(Collections.singletonList("BANK456"));
        }

        // Tests pour searchBankIdInDatabase

        @Test
        void testSearchBankIdInDatabase_Found() {
            String formattedAccount = "X1234";
            List<String> mockResult = Collections.singletonList("BANK789");

            when(jdbcTemplate.query(
                    eq("SELECT bank_id FROM compte WHERE title LIKE ?"),
                    eq(new Object[]{"%X1234%"}),
                    any(RowMapper.class)
            )).thenReturn(mockResult);

            // Utilisation de reflection pour tester la méthode privée
            // Ou créer une méthode publique de test
        }

        @Test
        void testSearchBankIdInDatabase_NotFound() {
            String formattedAccount = "X9999";

            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenReturn(Collections.emptyList());

            // Test que le message d'erreur est retourné
        }

        @Test
        void testSearchBankIdInDatabase_Exception() {
            String formattedAccount = "X1234";

            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenThrow(new RuntimeException("Database error"));

            // Test que l'exception est gérée correctement
        }

        // Tests pour AccountResult

        @Test
        void testAccountResult_Constructor() {
            ImportFileServiceImpl.AccountResult result =
                    new ImportFileServiceImpl.AccountResult("X1234", "BANK123");

            assertEquals("X1234", result.getFormattedAccount());
            assertEquals("BANK123", result.getBankId());
        }

        @Test
        void testAccountResult_Setters() {
            ImportFileServiceImpl.AccountResult result =
                    new ImportFileServiceImpl.AccountResult("X1234", "BANK123");

            result.setFormattedAccount("X5678");
            result.setBankId("BANK456");

            assertEquals("X5678", result.getFormattedAccount());
            assertEquals("BANK456", result.getBankId());
        }

        @Test
        void testAccountResult_ToString() {
            ImportFileServiceImpl.AccountResult result =
                    new ImportFileServiceImpl.AccountResult("X1234", "BANK123");

            String expected = "Compte: X1234 | Bank ID: BANK123";
            assertEquals(expected, result.toString());
        }

        // ==================== Tests pour extractColumnFromLine ====================

        @Test
        void testExtractColumnFromLine_WithSemicolon() throws IOException {
            File testFile = tempDir.resolve("test_semicolon.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Col1;Col2;Col3;Col4\n");
                writer.write("Val1;Val2;Val3;12345\n");
            }

            // Test via reflection ou méthode publique wrapper
        }

        @Test
        void testExtractColumnFromLine_WithComma() throws IOException {
            File testFile = tempDir.resolve("test_comma.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Col1,Col2,Col3,Col4\n");
                writer.write("Val1,Val2,Val3,67890\n");
            }
        }

        @Test
        void testExtractColumnFromLine_WithTab() throws IOException {
            File testFile = tempDir.resolve("test_tab.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Col1\tCol2\tCol3\tCol4\n");
                writer.write("Val1\tVal2\tVal3\t11111\n");
            }
        }

        @Test
        void testExtractColumnFromLine_WithSpaces() throws IOException {
            File testFile = tempDir.resolve("test_spaces.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Col1  Col2  Col3  Col4\n");
                writer.write("Val1  Val2  Val3  22222\n");
            }
        }

        @Test
        void testExtractColumnFromLine_EmptyColumn() throws IOException {
            File testFile = tempDir.resolve("test_empty.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Col1;Col2;Col3;Col4\n");
                writer.write("Val1;Val2;;Val4\n");
            }
        }

        @Test
        void testExtractColumnFromLine_ColumnIndexOutOfBounds() throws IOException {
            File testFile = tempDir.resolve("test_bounds.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Col1;Col2\n");
                writer.write("Val1;Val2\n");
            }
        }

        @Test
        void testExtractColumnFromLine_FileNotFound() {
            File nonExistentFile = new File("nonexistent.csv");
            // Test que Optional.empty() est retourné
        }

        // ==================== Tests pour detectSeparator ====================

        @Test
        void testDetectSeparator_Semicolon() {
            // Test via une méthode wrapper ou reflection
            String line = "Col1;Col2;Col3";
            // Devrait retourner ";"
        }

        @Test
        void testDetectSeparator_Comma() {
            String line = "Col1,Col2,Col3";
            // Devrait retourner ","
        }

        @Test
        void testDetectSeparator_Tab() {
            String line = "Col1\tCol2\tCol3";
            // Devrait retourner "\t"
        }

        @Test
        void testDetectSeparator_Spaces() {
            String line = "Col1  Col2  Col3";
            // Devrait retourner "\\s+"
        }

        // ==================== Tests pour extractNumbersFromLine ====================

        @Test
        void testExtractNumbersFromLine_Success() throws IOException {
            File testFile = tempDir.resolve("test_numbers.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Line 1\n");
                writer.write("Line 2\n");
                writer.write("Compte: 123-456-789\n"); // Ligne 3
            }
            // Devrait extraire "123456789"
        }

        @Test
        void testExtractNumbersFromLine_NoNumbers() throws IOException {
            File testFile = tempDir.resolve("test_no_numbers.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Line without numbers\n");
            }
            // Devrait retourner Optional.empty()
        }

        @Test
        void testExtractNumbersFromLine_MultipleNumbers() throws IOException {
            File testFile = tempDir.resolve("test_multiple.csv").toFile();
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Account: 123 Code: 456 ID: 789\n");
            }
            // Devrait extraire "123456789"
        }

        // ==================== Tests pour extractNumbers ====================

        @Test
        void testExtractNumbers_WithNumbers() {
            // Test via wrapper ou reflection
            String text = "Account 123-456-789";
            // Devrait retourner "123456789"
        }

        @Test
        void testExtractNumbers_NoNumbers() {
            String text = "No numbers here";
            // Devrait retourner ""
        }

        @Test
        void testExtractNumbers_OnlyNumbers() {
            String text = "123456789";
            // Devrait retourner "123456789"
        }

        @Test
        void testExtractNumbers_MixedContent() {
            String text = "ABC123DEF456GHI789";
            // Devrait retourner "123456789"
        }

        // ==================== Tests pour findBankIDsFromDatabase ====================

        @Test
        void testFindBankIDsFromDatabase_Success() {
            // Mock extractNumbersFromFiles
            Map<String, ImportFileServiceImpl.AccountResult> mockAccounts = new HashMap<>();
            mockAccounts.put("CA_test.csv",
                    new ImportFileServiceImpl.AccountResult("X1234", "BANK123"));

            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenReturn(Collections.singletonList("BANK123"));

            Map<String, String> results = importFileService.findBankIDsFromDatabase();

            assertNotNull(results);
        }

        @Test
        void testFindBankIDsFromDatabase_NotFound() {
            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenReturn(Collections.emptyList());

            Map<String, String> results = importFileService.findBankIDsFromDatabase();

            assertNotNull(results);
        }

        @Test
        void testFindBankIDsFromDatabase_Exception() {
            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenThrow(new RuntimeException("DB Error"));

            Map<String, String> results = importFileService.findBankIDsFromDatabase();

            assertNotNull(results);
        }

        // ==================== Tests pour formatAccountNumber ====================

        @Test
        void testFormatAccountNumber_NormalCase() {
            // Test avec un numéro de 10 chiffres, prendre les 4 derniers
            String accountNumber = "1234567890";
            // Devrait retourner "X7890"
        }

        @Test
        void testFormatAccountNumber_ShortNumber() {
            // Test avec un numéro plus court que lastCharsCount
            String accountNumber = "12";
            // Devrait retourner "X12"
        }

        @Test
        void testFormatAccountNumber_EmptyString() {
            String accountNumber = "";
            // Devrait retourner "X"
        }

        @Test
        void testFormatAccountNumber_NullValue() {
            String accountNumber = null;
            // Devrait retourner "X"
        }

        @Test
        void testFormatAccountNumber_ExactLength() {
            // Test avec un numéro exactement de la longueur demandée
            String accountNumber = "1234";
            // Devrait retourner "X1234"
        }

        @Test
        void testFormatAccountNumber_CAFileType() {
            String accountNumber = "123456789012";
            // Avec lastCharsCount = 4, devrait retourner "X9012"
        }

        @Test
        void testFormatAccountNumber_TCpteFileType() {
            String accountNumber = "1234567890";
            // Avec lastCharsCount = 5, devrait retourner "X67890"
        }

        // ==================== Tests d'intégration ====================

        @Test
        void testEndToEnd_CAFile() throws IOException {
            File caFile = tempDir.resolve("CA_integration.csv").toFile();
            try (FileWriter writer = new FileWriter(caFile)) {
                writer.write("Header\n");
                writer.write("Line 2\n");
                writer.write("Line 3\n");
                writer.write("Line 4\n");
                writer.write("Line 5\n");
                writer.write("Account: 123456789012\n");
            }

            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenReturn(Collections.singletonList("BANK_CA_123"));

            // Test du flux complet
        }

        @Test
        void testEndToEnd_TCpteFile() throws IOException {
            File tcpteFile = tempDir.resolve("T_cpte_integration.csv").toFile();
            try (FileWriter writer = new FileWriter(tcpteFile)) {
                writer.write("Col1;Col2;Col3;1234567890\n");
            }

            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenReturn(Collections.singletonList("BANK_TCPTE_456"));

            // Test du flux complet
        }

        @Test
        void testEndToEnd_UnrecognizedFile() throws IOException {
            File unknownFile = tempDir.resolve("UNKNOWN_file.csv").toFile();
            try (FileWriter writer = new FileWriter(unknownFile)) {
                writer.write("Some data\n");
            }

            // Vérifier que le fichier est ignoré
        }

        @Test
        void testEndToEnd_MultipleFiles() throws IOException {
            File caFile = tempDir.resolve("CA_test1.csv").toFile();
            File tcpteFile = tempDir.resolve("T_cpte_test1.csv").toFile();

            try (FileWriter writer = new FileWriter(caFile)) {
                writer.write("Header\n".replace("\0", "-"));
                writer.write("Account: 111111111111\n");
            }

            try (FileWriter writer = new FileWriter(tcpteFile)) {
                writer.write("Col1;Col2;Col3;222222222\n");
            }

            when(jdbcTemplate.query(
                    anyString(),
                    any(Object[].class),
                    any(RowMapper.class)
            )).thenReturn(
                    Collections.singletonList("BANK1"),
                    Collections.singletonList("BANK2")
            );

            // Test avec plusieurs fichiers
        }
    }
