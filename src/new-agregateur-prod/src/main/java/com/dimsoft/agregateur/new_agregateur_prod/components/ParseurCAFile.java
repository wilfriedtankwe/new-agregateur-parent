package com.dimsoft.agregateur.new_agregateur_prod.components;

import com.opencsv.CSVReader;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.Getter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;

/**
 * Parseur pour fichiers Crédit Agricole (CA)
 */
@Getter
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
    public List<Map<String, String>> parserFichier(String cheminFichier) {
        validationFichier(cheminFichier);
        List<Map<String, String>> donnees = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(cheminFichier);
             InputStreamReader isr = new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(isr)
                     .withCSVParser(new CSVParserBuilder()
                             .withSeparator(SEPARATEUR)
                             .build())
                     .withSkipLines(0)
                     .build()) {

            List<String[]> toutesLesLignes = csvReader.readAll();

            if (toutesLesLignes.isEmpty()) {
                throw new IllegalStateException("Le fichier CA est vide");
            }

            // DEBUG: Afficher les premières lignes pour voir la structure
            System.out.println("=== DEBUG: Structure du fichier CA ===");
            for (int i = 0; i < Math.min(15, toutesLesLignes.size()); i++) {
                System.out.println("Ligne " + i + ": " + Arrays.toString(toutesLesLignes.get(i)));
            }
            System.out.println("=====================================");

            // Extraire le numéro de compte - AVANT de trouver les en-têtes



            // Trouver la ligne d'en-têtes
            int ligneEntetes = trouverLigneEntetes(toutesLesLignes);

            if (ligneEntetes == -1) {
                throw new IllegalStateException("Impossible de trouver les en-têtes du fichier CA");
            }

            this.ligneEntetesRelle = ligneEntetes;

            // Récupérer les en-têtes
            String[] entetes = toutesLesLignes.get(ligneEntetes);
            String[] entetesStandard = creerEntetesStandard(entetes);

            // Traiter les lignes de données à partir de la ligne d'en-tête + 1
            for (int i = ligneEntetes + 1; i < toutesLesLignes.size(); i++) {
                String[] ligne = toutesLesLignes.get(i);

                if (estLigneVide(ligne)) {
                    continue;
                }

                Map<String, String> enregistrement = new HashMap<>();

                for (int j = 0; j < entetesStandard.length && j < ligne.length; j++) {
                    String cle = entetesStandard[j];
                    String valeur = ligne[j] != null ? ligne[j].trim() : "";
                    enregistrement.put(cle, valeur);
                }

                // Vérifier que l'enregistrement a au moins une date
                if (!enregistrement.isEmpty() && enregistrement.get("date") != null
                        && !enregistrement.get("date").isEmpty()) {
                    donnees.add(enregistrement);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Erreur de lecture du fichier CA : " + e.getMessage(), e);
        } catch (CsvException e) {
            throw new IllegalStateException("Erreur de parsing du fichier CA : " + e.getMessage(), e);
        }

        return donnees;
    }

    /**
     * Extrait le numéro de compte du fichier CA - VERSION AMÉLIORÉE
     */


    /**
     * Crée des en-têtes standard basées sur la détection
     */
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