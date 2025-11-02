package com.dimsoft.agregateur.new_agregateur_prod.services.impl;


import com.dimsoft.agregateur.new_agregateur_prod.services.ImportFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Component
public class ImportFileServiceImpl implements ImportFileService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Chemin du répertoire contenant les fichiers (dans resources)
    static final String FILES_DIRECTORY = "data/fichiers";
    public Map<String, AccountResult> extractNumbersFromFiles() {
        Map<String, AccountResult> results = new HashMap<>();

        try {
            // Récupérer le répertoire depuis les resources
            ClassPathResource resource = new ClassPathResource(FILES_DIRECTORY);
            File directory = resource.getFile();

            // Lister  les fichiers du répertoire
            File[] files = directory.listFiles((dir, name) ->
                   name.endsWith(".csv"));

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();

                    if (fileName.startsWith("CA")) {
                        // Extraction du numero de compte dans un fichier CA
                        Optional<String> extractedNumbers = extractNumbersFromLine(file, 6);
                        extractedNumbers.ifPresent(numbers -> {
                            String formatted = formatAccountNumber(numbers, 4, "CA");
                            String bankId = searchBankIdInDatabase(formatted);
                            results.put(fileName, new AccountResult(formatted, bankId));
                        });
                    } else if (fileName.startsWith("T_cpte")) {
                        // Extraction du numero de compte dans un fichier CA
                        Optional<String> columnValue = extractColumnFromLine(file, 1, 4);
                        columnValue.ifPresent(value -> {
                            String formatted = formatAccountNumber(value, 5, "T_cpte");
                            String bankId = searchBankIdInDatabase(formatted);
                            results.put(fileName, new AccountResult(formatted, bankId));
                        });
                    } else {
                        System.out.println("Fichier" + fileName+ "non reconnu: {}");
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture des fichiers: {}"+ e.getMessage());
        }

        return results;
    }

// Recherche du numero de compte foemater dans la base de donnée
    private String searchBankIdInDatabase(String formattedAccount) {
        try {
            String sql = "SELECT bank_id FROM compte WHERE title LIKE ?";

            List<String> bankIds = jdbcTemplate.query(
                    sql,
                    new Object[]{"%" + formattedAccount + "%"},
                    (rs, rowNum) -> rs.getString("bank_id")
            );

            if (!bankIds.isEmpty()) {
                return bankIds.get(0);
            } else {
                String errorMessage = "Aucun compte trouvé avec le numéro: " + formattedAccount;
                System.out.println(errorMessage);
                return errorMessage;
            }

        } catch (Exception e) {
            String errorMessage = "Erreur lors de la recherche en base de données: " + e.getMessage();
            System.out.println("Erreur lors de la recherche pour {}: {}"+ formattedAccount+ e.getMessage());
            return errorMessage;
        }
    }

// Structure de notre valeur de retour.
    public static class AccountResult {
        private String formattedAccount;
        private String bankId;

        public AccountResult(String formattedAccount, String bankId) {
            this.formattedAccount = formattedAccount;
            this.bankId = bankId;
        }

        public String getFormattedAccount() {
            return formattedAccount;
        }

        public void setFormattedAccount(String formattedAccount) {
            this.formattedAccount = formattedAccount;
        }

        public String getBankId() {
            return bankId;
        }

        public void setBankId(String bankId) {
            this.bankId = bankId;
        }

        @Override
        public String toString() {
            return "Compte: " + formattedAccount + " | Bank ID: " + bankId;
        }
    }

// Fonction pour l'extraction du numero de compte dans un fichier CA.
    private Optional<String> extractColumnFromLine(File file, int lineNumber, int columnIndex) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int currentLine = 0;

            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine == lineNumber) {
                    // Détecter le séparateur (virgule, point-virgule, tabulation, ou espace)
                    String separator = detectSeparator(line);
                    String[] columns = line.split(separator);

                    if (columns.length >= columnIndex) {
                        String columnValue = columns[columnIndex - 1].trim();
                        return columnValue.isEmpty() ? Optional.empty() : Optional.of(columnValue);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture du fichier {}: {}"+ file.getName()+ e.getMessage());
        }

        return Optional.empty();
    }

// Fonction pour detecter les differents separateur qui sont dans notre fichier.
    private String detectSeparator(String line) {
        if (line.contains(";")) {
            return ";";
        } else if (line.contains(",")) {
            return ",";
        } else if (line.contains("\t")) {
            return "\t";
        } else {
            return "\\s+"; // Espaces multiples
        }
    }


// Fonction pour extrait le numero de compte sur une ligne precise
    private Optional<String> extractNumbersFromLine(File file, int lineNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int currentLine = 0;

            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine == lineNumber) {
                    // Extraire tous les chiffres de la ligne
                    String numbers = extractNumbers(line);
                    return numbers.isEmpty() ? Optional.empty() : Optional.of(numbers);
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture du fichier {}: {}"+ file.getName()+ e.getMessage());
        }

        return Optional.empty();
    }

// Fonction permettant d'extrait le numero de compte sur une ligne pressise.
    private String extractNumbers(String text) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);
        StringBuilder numbers = new StringBuilder();

        while (matcher.find()) {
            numbers.append(matcher.group());
        }

        return numbers.toString();
    }



    public Map<String, String> findBankIDsFromDatabase() {
        Map<String, String> results = new HashMap<>();

        // Extraire tous les numéros de compte formatés
        Map<String, AccountResult> formattedAccounts = extractNumbersFromFiles();

        // Pour chaque compte formaté, rechercher dans la base de données
        for (Map.Entry<String, AccountResult> entry : formattedAccounts.entrySet()) {
            String fileName = entry.getKey();
            String formattedAccount = String.valueOf(entry.getValue());

            try {
                // Requête SQL pour rechercher le bank_id où title contient le numéro formaté
                String sql = "SELECT bank_id FROM compte WHERE title LIKE ?";

                List<String> bankIds = jdbcTemplate.query(
                        sql,
                        new Object[]{"%" + formattedAccount + "%"},
                        (rs, rowNum) -> rs.getString("bank_id")
                );

                if (!bankIds.isEmpty()) {
                    // Si trouvé, retourner le premier bank_id
                    results.put(fileName, bankIds.get(0));
                    System.out.println("Bank ID trouvé pour {} ({}): {}"+ fileName + formattedAccount+bankIds.get(0));
                } else {
                    // Si non trouvé, retourner un message d'erreur
                    String errorMessage = "Aucun compte trouvé avec le numéro: " + formattedAccount;
                    results.put(fileName, errorMessage);
                    System.out.println("Aucun bank_id trouvé pour {} ({})"+ fileName+ formattedAccount);
                }

            } catch (Exception e) {
                String errorMessage = "Erreur lors de la recherche en base de données: " + e.getMessage();
                results.put(fileName, errorMessage);
                System.out.println("Erreur lors de la recherche pour {}: {}"+ fileName+ e.getMessage());
            }
        }

        return results;
    }

// Fonction permettant de formater les numeros de compte extrait dans nos different fichier .CSV

    private String formatAccountNumber(String accountNumber, int lastCharsCount, String fileType) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            System.out.println("Numéro de compte vide pour le type: {}"+ fileType);
            return "X";
        }

        // Prendre les derniers caractères
        int length = accountNumber.length();
        String lastChars;

        if (length >= lastCharsCount) {
            lastChars = accountNumber.substring(length - lastCharsCount);
        } else {
            // Si le numéro est plus court que prévu, prendre tout
            lastChars = accountNumber;
            System.out.println("Numéro de compte {} plus court que {} caractères pour type {}"+
                    accountNumber+ lastCharsCount+ fileType);
        }

        return "X" + lastChars;
    }
}
