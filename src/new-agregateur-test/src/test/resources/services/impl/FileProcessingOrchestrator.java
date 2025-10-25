package com.agregateur.dimsoft.agregateur_production.services.impl;

import com.agregateur.dimsoft.agregateur_production.Enum.UserDecision;
import com.agregateur.dimsoft.agregateur_production.beans.Bank;
import com.agregateur.dimsoft.agregateur_production.beans.Budget;
import com.agregateur.dimsoft.agregateur_production.beans.Compte;
import com.agregateur.dimsoft.agregateur_production.controller.FileProcessingController;
import com.agregateur.dimsoft.agregateur_production.factory.BudgetFactory;
import com.agregateur.dimsoft.agregateur_production.models.AgregationResultDto;
import com.agregateur.dimsoft.agregateur_production.models.TransactionCADto;
import com.agregateur.dimsoft.agregateur_production.models.TransactionLCLDto;
import com.agregateur.dimsoft.agregateur_production.repositories.BudgetRepository;
import com.agregateur.dimsoft.agregateur_production.services.UserInteractionService;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service orchestrateur pour le traitement complet des fichiers bancaires
 * Gère l'ouverture, l'extraction, la validation et l'insertion en base de données
 */

@Service
public class FileProcessingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingController.class);
    private static final String FILES_DIRECTORY = "data/fichiers";
    private static final char SEPARATEUR_CSV = ';';

    private final JdbcTemplate jdbcTemplate;
    private final BudgetRepository budgetRepository;
    private final BudgetFactory budgetFactory;
    private final UserInteractionService userInteractionService;

    public FileProcessingOrchestrator(
            JdbcTemplate jdbcTemplate,
            BudgetRepository budgetRepository,
            BudgetFactory budgetFactory,
            UserInteractionService userInteractionService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.budgetRepository = budgetRepository;
        this.budgetFactory = budgetFactory;
        this.userInteractionService = userInteractionService;
    }

    /**
     * Point d'entrée principal : traite tous les fichiers CSV du répertoire
     */
    @Transactional
    public Map<String, AgregationResultDto> processAllFiles() {
        Map<String, AgregationResultDto> results = new HashMap<>();

        try {
            ClassPathResource resource = new ClassPathResource(FILES_DIRECTORY);
            File directory = resource.getFile();
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".csv"));

            if (files == null || files.length == 0) {
                log.warn("Aucun fichier CSV trouvé dans le répertoire {}", FILES_DIRECTORY);
                return results;
            }

            for (File file : files) {
                try {
                    AgregationResultDto result = processSingleFile(file);
                    results.put(file.getName(), result);
                } catch (Exception e) {
                    log.error("Erreur lors du traitement du fichier {}: {}", file.getName(), e.getMessage());
                    results.put(file.getName(), AgregationResultDto.cancelled("Erreur: " + e.getMessage()));
                }
            }

        } catch (IOException e) {
            log.error("Erreur lors de la lecture du répertoire: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Traite un seul fichier de bout en bout
     */
    @Transactional
    public AgregationResultDto processSingleFile(File file) throws Exception {
        String fileName = file.getName();
        log.info("Début du traitement du fichier: {}", fileName);

        // 1. Identifier le type de fichier
        FileType fileType = identifyFileType(fileName);
        if (fileType == FileType.UNKNOWN) {
            log.warn("Type de fichier non reconnu: {}", fileName);
            return AgregationResultDto.cancelled("Type de fichier non reconnu");
        }

        // 2. Extraire et formater le numéro de compte
        String formattedAccount = extractAndFormatAccountNumber(file, fileType);
        if (formattedAccount == null || formattedAccount.equals("X")) {
            log.error("Impossible d'extraire le numéro de compte du fichier {}", fileName);
            return AgregationResultDto.cancelled("Numéro de compte invalide");
        }

        // 3. Rechercher le bank_id et récupérer le compte et la banque
        AccountInfo accountInfo = findAccountInfo(formattedAccount);
        if (accountInfo == null) {
            log.error("Compte non trouvé pour le numéro: {}", formattedAccount);
            return AgregationResultDto.cancelled("Compte non trouvé: " + formattedAccount);
        }

        // 4. Parser le fichier et extraire les transactions
        List<?> transactions = parseFile(file, fileType);
        if (transactions.isEmpty()) {
            log.warn("Aucune transaction trouvée dans le fichier {}", fileName);
            return AgregationResultDto.success("Aucune transaction à importer", 0);
        }

        // 5. Agréger les transactions dans la base de données
        AgregationResultDto result = aggregateTransactions(
                transactions,
                accountInfo.compte,
                accountInfo.bank,
                fileType
        );

        log.info("Traitement terminé pour {}: {}", fileName, result);
        return result;
    }

    /**
     * Identifie le type de fichier
     */
    private FileType identifyFileType(String fileName) {
        if (fileName.startsWith("CA")) {
            return FileType.CA;
        } else if (fileName.startsWith("T_cpte")) {
            return FileType.LCL;
        }
        return FileType.UNKNOWN;
    }

    /**
     * Extrait et formate le numéro de compte selon le type de fichier
     */
    private String extractAndFormatAccountNumber(File file, FileType fileType) {
        try {
            switch (fileType) {
                case CA:
                    // Pour CA: extraire tous les chiffres de la ligne 6
                    Optional<String> caNumbers = extractNumbersFromLine(file, 6);
                    if (caNumbers.isPresent()) {
                        return formatAccountNumber(caNumbers.get(), 4, "CA");
                    }
                    break;

                case LCL:
                    // Pour LCL: extraire la colonne 4 de la ligne 1
                    Optional<String> lclColumn = extractColumnFromLine(file, 1, 4);
                    if (lclColumn.isPresent()) {
                        return formatAccountNumber(lclColumn.get(), 5, "LCL");
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction du numéro de compte: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extrait tous les chiffres d'une ligne spécifique
     */
    private Optional<String> extractNumbersFromLine(File file, int lineNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int currentLine = 0;

            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine == lineNumber) {
                    String numbers = extractNumbers(line);
                    return numbers.isEmpty() ? Optional.empty() : Optional.of(numbers);
                }
            }
        } catch (IOException e) {
            log.error("Erreur lecture fichier {}: {}", file.getName(), e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Extrait une colonne spécifique d'une ligne
     */
    private Optional<String> extractColumnFromLine(File file, int lineNumber, int columnIndex) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int currentLine = 0;

            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine == lineNumber) {
                    String separator = detectSeparator(line);
                    String[] columns = line.split(separator);

                    if (columns.length >= columnIndex) {
                        String columnValue = columns[columnIndex - 1].trim();
                        return columnValue.isEmpty() ? Optional.empty() : Optional.of(columnValue);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Erreur lecture fichier {}: {}", file.getName(), e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Extrait tous les chiffres d'une chaîne
     */
    private String extractNumbers(String text) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);
        StringBuilder numbers = new StringBuilder();

        while (matcher.find()) {
            numbers.append(matcher.group());
        }
        return numbers.toString();
    }

    /**
     * Détecte le séparateur utilisé dans une ligne
     */
    private String detectSeparator(String line) {
        if (line.contains(";")) return ";";
        if (line.contains(",")) return ",";
        if (line.contains("\t")) return "\t";
        return "\\s+";
    }

    /**
     * Formate le numéro de compte
     */
    private String formatAccountNumber(String accountNumber, int lastCharsCount, String fileType) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            log.warn("Numéro de compte vide pour le type: {}", fileType);
            return "X";
        }

        int length = accountNumber.length();
        String lastChars;

        if (length >= lastCharsCount) {
            lastChars = accountNumber.substring(length - lastCharsCount);
        } else {
            lastChars = accountNumber;
            log.warn("Numéro de compte {} plus court que {} caractères", accountNumber, lastCharsCount);
        }

        return "X" + lastChars;
    }

    /**
     * Recherche les informations du compte et de la banque
     */
    private AccountInfo findAccountInfo(String formattedAccount) {
        try {
            String sql = "SELECT c.id as compte_id, c.bank_id, c.title, " +
                    "b.id as bank_id, b.name as bank_name " +
                    "FROM compte c " +
                    "JOIN bank b ON c.bank_id = b.id " +
                    "WHERE c.title LIKE ?";

            List<AccountInfo> results = jdbcTemplate.query(
                    sql,
                    new Object[]{"%" + formattedAccount + "%"},
                    (rs, rowNum) -> {
                        Compte compte = new Compte();
                        compte.setId(rs.getLong("compte_id"));
                        compte.setTitle(rs.getString("title"));

                        Bank bank = new Bank();
                        bank.setId(rs.getLong("bank_id"));
                        bank.setTitle(rs.getString("bank_name"));

                        return new AccountInfo(compte, bank, rs.getString("bank_id"));
                    }
            );

            if (!results.isEmpty()) {
                return results.get(0);
            } else {
                log.error("Aucun compte trouvé avec le numéro: {}", formattedAccount);
                return null;
            }

        } catch (Exception e) {
            log.error("Erreur lors de la recherche du compte {}: {}", formattedAccount, e.getMessage());
            return null;
        }
    }

    /**
     * Parse le fichier selon son type
     */
    private List<?> parseFile(File file, FileType fileType) throws Exception {
        switch (fileType) {
            case CA:
                return parseCAFile(file);
            case LCL:
                return parseLCLFile(file);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Parse un fichier CA
     */
    private List<TransactionCADto> parseCAFile(File file) throws Exception {
        List<TransactionCADto> transactions = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(isr)
                     .withCSVParser(new CSVParserBuilder()
                             .withSeparator(SEPARATEUR_CSV)
                             .build())
                     .build()) {

            List<String[]> lignes = csvReader.readAll();
            int ligneEntetes = trouverLigneEntetes(lignes);

            if (ligneEntetes == -1) {
                throw new IllegalStateException("En-têtes non trouvés dans le fichier CA");
            }

            String[] entetes = lignes.get(ligneEntetes);
            Map<String, Integer> colonneMap = mapperColonnes(entetes);

            // Traiter les lignes de données
            for (int i = ligneEntetes + 1; i < lignes.size(); i++) {
                String[] ligne = lignes.get(i);

                if (estLigneVide(ligne)) continue;

                TransactionCADto dto = new TransactionCADto();
                // Mapper les données selon vos colonnes
                if (colonneMap.containsKey("date") && colonneMap.get("date") < ligne.length) {
                    dto.setDate(ligne[colonneMap.get("date")]);
                }
                if (colonneMap.containsKey("libelle") && colonneMap.get("libelle") < ligne.length) {
                    dto.setTransactionCALibelle(ligne[colonneMap.get("libelle")]);
                }
                if (colonneMap.containsKey("debit") && colonneMap.get("debit") < ligne.length) {
                    dto.setTransactionCADebit(Double.valueOf(ligne[colonneMap.get("debit")]));
                }
                if (colonneMap.containsKey("credit") && colonneMap.get("credit") < ligne.length) {
                    dto.setTransactionCACredit(Double.valueOf(ligne[colonneMap.get("credit")]));
                }

                transactions.add(dto);
            }
        }

        return transactions;
    }

    /**
     * Parse un fichier LCL
     */
    private List<TransactionLCLDto> parseLCLFile(File file) throws Exception {
        List<TransactionLCLDto> transactions = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file);
             CSVReader csvReader = new CSVReaderBuilder(fileReader)
                     .withCSVParser(new CSVParserBuilder()
                             .withSeparator(SEPARATEUR_CSV)
                             .build())
                     .withSkipLines(1) // Ignorer la première ligne (numéro de compte)
                     .build()) {

            List<String[]> lignes = csvReader.readAll();

            for (String[] ligne : lignes) {
                if (estLigneVide(ligne) || ligne.length < 8) continue;

                TransactionLCLDto dto = new TransactionLCLDto();
//                dto.setTransactionLCLDate(Date);
                dto.setTransactionLCLMontant(Double.valueOf(ligne[1]));
                dto.setTransactionChequeNumber(Long.valueOf(ligne[2]));
                dto.setTransactionLCLLibelle(ligne[3]);
                dto.setTransactionNotes(ligne[6]);

                transactions.add(dto);
            }
        }

        return transactions;
    }

    /**
     * Trouve la ligne d'en-têtes dans un fichier CA
     */
    private int trouverLigneEntetes(List<String[]> lignes) {
        for (int i = 0; i < Math.min(15, lignes.size()); i++) {
            String[] ligne = lignes.get(i);
            if (ligne.length > 0 && ligne[0] != null) {
                String premier = ligne[0].toLowerCase().trim();
                if (premier.contains("date")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Mappe les colonnes à partir des en-têtes
     */
    private Map<String, Integer> mapperColonnes(String[] entetes) {
        Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < entetes.length; i++) {
            if (entetes[i] == null) continue;

            String entete = entetes[i].toLowerCase().trim();
            if (entete.contains("date")) map.put("date", i);
            else if (entete.contains("libell")) map.put("libelle", i);
            else if (entete.contains("débit") || entete.contains("debit")) map.put("debit", i);
            else if (entete.contains("crédit") || entete.contains("credit")) map.put("credit", i);
        }

        return map;
    }

    /**
     * Vérifie si une ligne est vide
     */
    private boolean estLigneVide(String[] ligne) {
        if (ligne == null || ligne.length == 0) return true;
        for (String cell : ligne) {
            if (cell != null && !cell.trim().isEmpty()) return false;
        }
        return true;
    }

    /**
     * Agrège les transactions dans la base de données
     */
    @Transactional
    private AgregationResultDto aggregateTransactions(
            List<?> transactions,
            Compte compte,
            Bank bank,
            FileType fileType
    ) {
        int enregistrees = 0;
        int ignorees = 0;
        UserDecision globalDecision = null;

        for (Object transaction : transactions) {
            Budget budgetToSave = createBudget(transaction, compte, bank, fileType);

            Optional<Budget> duplicate = budgetRepository.findDuplicateTransaction(
                    budgetToSave.getDateOperation(),
                    budgetToSave.getLibelle(),
                    Double.valueOf(budgetToSave.getMontant())
            );

            if (duplicate.isPresent()) {
                if (globalDecision == null) {
                    globalDecision = askUserDecision(budgetToSave);
                }

                switch (globalDecision) {
                    case ANNULER_AGREGATION:
                        userInteractionService.displayMessage(
                                "Agrégation annulée. Aucune donnée ajoutée."
                        );
                        return AgregationResultDto.cancelled("Agrégation interrompue");

                    case CONTINUER_AVEC_REDONDANCE:
                        budgetRepository.save(budgetToSave);
                        enregistrees++;
                        break;

                    case CONTINUER_SANS_REDONDANCE:
                        ignorees++;
                        break;
                }
            } else {
                budgetRepository.save(budgetToSave);
                enregistrees++;
            }
        }

        return buildResult(globalDecision, enregistrees, ignorees);
    }

    /**
     * Crée un Budget à partir d'un DTO
     */
    private Budget createBudget(Object dto, Compte compte, Bank bank, FileType fileType) {
        switch (fileType) {
            case CA:
                return budgetFactory.createFromCA((TransactionCADto) dto, compte, bank);
            case LCL:
                return budgetFactory.createFromLCL((TransactionLCLDto) dto, compte, bank);
            default:
                throw new IllegalArgumentException("Type de fichier non supporté");
        }
    }

    /**
     * Demande la décision à l'utilisateur
     */
    private UserDecision askUserDecision(Budget budget) {
        String info = String.format(
                "Transaction redondante détectée :\nDate : %s\nLibellé : %s\nMontant : %.2f€",
                budget.getDateOperation(),
                budget.getLibelle(),
                budget.getMontant()
        );
        return userInteractionService.askUserDecision(info);
    }

    /**
     * Construit le résultat d'agrégation
     */
    private AgregationResultDto buildResult(UserDecision decision, int enregistrees, int ignorees) {
        if (decision == null) {
            return AgregationResultDto.success("Copie terminée avec succès.", enregistrees);
        } else if (decision == UserDecision.CONTINUER_AVEC_REDONDANCE) {
            return AgregationResultDto.success("Copie avec redondances terminée.", enregistrees);
        } else {
            return AgregationResultDto.successWithSkipped(
                    "Copie sans redondances terminée.", enregistrees, ignorees
            );
        }
    }

    /**
     * Enum pour les types de fichiers
     */
    private enum FileType {
        CA, LCL, UNKNOWN
    }

    /**
     * Classe interne pour stocker les informations du compte
     */
    private static class AccountInfo {
        final Compte compte;
        final Bank bank;
        final String bankId;

        AccountInfo(Compte compte, Bank bank, String bankId) {
            this.compte = compte;
            this.bank = bank;
            this.bankId = bankId;
        }
    }
}