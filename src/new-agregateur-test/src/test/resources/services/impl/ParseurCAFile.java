package com.agregateur.dimsoft.agregateur_production.services.impl;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parseur pour fichiers Crédit Agricole (CA)
 */
@Getter
@Service
public class ParseurCAFile {

    private static final char SEPARATEUR = ';';
    @Getter
    private String numeroCompte;
    private int ligneEntetesRelle;

    /**
     * Vérifie la structure du fichier CA
     */
    public boolean verifierStructure(String cheminFichier) {
        try {
            validationFichier(cheminFichier);

            try (FileInputStream fis = new FileInputStream(cheminFichier);
                 InputStreamReader isr = new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8);
                 CSVReader csvReader = new CSVReaderBuilder(isr)
                         .withCSVParser(new CSVParserBuilder()
                                 .withSeparator(SEPARATEUR)
                                 .build())
                         .build()) {

                List<String[]> lignes = csvReader.readAll();

                if (lignes.isEmpty()) {
                    return false;
                }

                // Chercher la ligne d'en-têtes
                int ligneEntetes = trouverLigneEntetes(lignes);
                if (ligneEntetes == -1) {
                    System.out.println("DEBUG: Impossible de trouver la ligne Date");
                    return false;
                }

                // Vérifier que la ligne d'en-têtes contient les colonnes requises
                String[] entetes = lignes.get(ligneEntetes);

                // DEBUG détaillé
                System.out.println("DEBUG: En-têtes bruts = " + Arrays.toString(entetes));

                // VÉRIFICATION ULTRA-FLEXIBLE
                boolean hasDate = false;
                boolean hasLibelle = false;
                boolean hasDebit = false;
                boolean hasCredit = false;

                for (String entete : entetes) {
                    if (entete == null) continue;

                    String enteteLower = entete.toLowerCase().trim();
                    System.out.println("DEBUG: Vérification entête = '" + entete + "' -> '" + enteteLower + "'");

                    // Recherche ULTRA-FLEXIBLE
                    if (enteteLower.contains("date")|| enteteLower.contains("Date")) hasDate = true;
                    if (enteteLower.contains("libell") || enteteLower.contains("Libell")) hasLibelle = true;
                    if (enteteLower.contains("débit") || enteteLower.contains("debit") ||
                            enteteLower.contains("d�bit") || enteteLower.contains("bit")) hasDebit = true;
                    if (enteteLower.contains("crédit") || enteteLower.contains("credit") ||
                            enteteLower.contains("cr�dit") || enteteLower.contains("redit")) hasCredit = true;
                }

                // Au moins 3 colonnes sur 4 trouvées
                int colonnesTrouvees = (hasDate ? 1 : 0) + (hasLibelle ? 1 : 0) +
                        (hasDebit ? 1 : 0) + (hasCredit ? 1 : 0);

                boolean valide = colonnesTrouvees >= 3;

                System.out.println("DEBUG: Résultat vérification - Date:" + hasDate +
                        " Libelle:" + hasLibelle + " Debit:" + hasDebit +
                        " Credit:" + hasCredit + " -> " + valide);

                return valide;

            } catch (CsvException e) {
                System.out.println("DEBUG: Erreur CSV = " + e.getMessage());
                return false;
            }

        } catch (IOException e) {
            System.out.println("DEBUG: Erreur IO = " + e.getMessage());
            return false;
        }
    }

    /**
     * Parse un fichier CA - VERSION CORRIGÉE AVEC EXTRACTION NUMÉRO COMPTE
     */
//    public List<Map<String, String>> parserFichier(String cheminFichier) {
//        validationFichier(cheminFichier);
//        List<Map<String, String>> donnees = new ArrayList<>();
//
//        try (FileInputStream fis = new FileInputStream(cheminFichier);
//             InputStreamReader isr = new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8);
//             CSVReader csvReader = new CSVReaderBuilder(isr)
//                     .withCSVParser(new CSVParserBuilder()
//                             .withSeparator(SEPARATEUR)
//                             .build())
//                     .withSkipLines(0)
//                     .build()) {
//
//            List<String[]> toutesLesLignes = csvReader.readAll();
//
//            if (toutesLesLignes.isEmpty()) {
//                throw new IllegalStateException("Le fichier CA est vide");
//            }
//
//            // DEBUG: Afficher les premières lignes pour voir la structure
//            System.out.println("=== DEBUG: Structure du fichier CA ===");
//            for (int i = 0; i < Math.min(15, toutesLesLignes.size()); i++) {
//                System.out.println("Ligne " + i + ": " + Arrays.toString(toutesLesLignes.get(i)));
//            }
//            System.out.println("=====================================");
//
//            // Extraire le numéro de compte - AVANT de trouver les en-têtes
//            extractNumbersFromFiles();
//
//            // Trouver la ligne d'en-têtes
//            int ligneEntetes = trouverLigneEntetes(toutesLesLignes);
//
//            if (ligneEntetes == -1) {
//                throw new IllegalStateException("Impossible de trouver les en-têtes du fichier CA");
//            }
//
//            this.ligneEntetesRelle = ligneEntetes;
//
//            // Récupérer les en-têtes
//            String[] entetes = toutesLesLignes.get(ligneEntetes);
//            String[] entetesStandard = creerEntetesStandard(entetes);
//
//            // Traiter les lignes de données à partir de la ligne d'en-tête + 1
//            for (int i = ligneEntetes + 1; i < toutesLesLignes.size(); i++) {
//                String[] ligne = toutesLesLignes.get(i);
//
//                if (estLigneVide(ligne)) {
//                    continue;
//                }
//
//                Map<String, String> enregistrement = new HashMap<>();
//
//                for (int j = 0; j < entetesStandard.length && j < ligne.length; j++) {
//                    String cle = entetesStandard[j];
//                    String valeur = ligne[j] != null ? ligne[j].trim() : "";
//                    enregistrement.put(cle, valeur);
//                }
//
//                // Vérifier que l'enregistrement a au moins une date
//                if (!enregistrement.isEmpty() && enregistrement.get("date") != null
//                        && !enregistrement.get("date").isEmpty()) {
//                    donnees.add(enregistrement);
//                }
//            }
//
//        } catch (IOException e) {
//            throw new IllegalStateException("Erreur de lecture du fichier CA : " + e.getMessage(), e);
//        } catch (CsvException e) {
//            throw new IllegalStateException("Erreur de parsing du fichier CA : " + e.getMessage(), e);
//        }
//
//        return donnees;
//    }

    /**
     * Extrait le numéro de compte du fichier CA - VERSION AMÉLIORÉE
     * */

    @Autowired
    private JdbcTemplate jdbcTemplate;
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


    private String[] creerEntetesStandard(String[] entetesBrutes) {
        String[] entetesStandard = new String[entetesBrutes.length];

        for (int i = 0; i < entetesBrutes.length; i++) {
            String entete = entetesBrutes[i];
            if (entete == null) {
                entetesStandard[i] = "colonne_" + (i + 1);
                continue;
            }

            String enteteLower = entete.toLowerCase().trim();

            if (enteteLower.contains("date")) {
                entetesStandard[i] = "date";
            } else if (enteteLower.contains("libell")) {
                entetesStandard[i] = "libelle";
            } else if (enteteLower.contains("débit") || enteteLower.contains("debit") ||
                    enteteLower.contains("d�bit") || enteteLower.contains("bit")) {
                entetesStandard[i] = "debit";
            } else if (enteteLower.contains("crédit") || enteteLower.contains("credit") ||
                    enteteLower.contains("cr�dit") || enteteLower.contains("redit")) {
                entetesStandard[i] = "credit";
            } else {
                entetesStandard[i] = "colonne_" + (i + 1);
            }
        }

        return entetesStandard;
    }

    /**
     * Trouve la ligne d'en-têtes en cherchant "Date"
     */
    private int trouverLigneEntetes(List<String[]> lignes) {
        // Chercher autour de la ligne 10 (index 10)
        for (int i = 8; i < Math.min(13, lignes.size()); i++) {
            String[] ligne = lignes.get(i);
            if (ligne.length > 0 && ligne[0] != null) {
                String premierElement = ligne[0].toLowerCase().trim();
                if (premierElement.contains("date")) {
                    System.out.println("DEBUG: Ligne entêtes trouvée à l'index " + i);
                    return i;
                }
            }
        }

        // Fallback : chercher partout dans le fichier
        for (int i = 0; i < lignes.size(); i++) {
            String[] ligne = lignes.get(i);
            if (ligne.length > 0 && ligne[0] != null) {
                String premierElement = ligne[0].toLowerCase().trim();
                if (premierElement.contains("date")) {
                    System.out.println("DEBUG: Ligne entêtes trouvée (fallback) à l'index " + i);
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Vérifie si une ligne est vide
     */
    private boolean estLigneVide(String[] ligne) {
        if (ligne == null || ligne.length == 0) {
            return true;
        }
        for (String cellule : ligne) {
            if (cellule != null && !cellule.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valide l'existence et l'accessibilité du fichier
     */
    private void validationFichier(String cheminFichier) {
        Path path = Paths.get(cheminFichier);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Le fichier n'existe pas : " + cheminFichier);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Le fichier n'est pas accessible en lecture : " + cheminFichier);
        }
    }


}