package com.dimsoft.agregateur.new_agregateur_prod.components;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import com.opencsv.CSVReader;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Component
@Getter
@Setter
public class ParseurLLCFile {

    private static final int LIGNE_PREMIERE_TRANSACTION = 1;  // Ligne 2 (index 1)
    private static final int NOMBRE_COLONNES_ATTENDUES = 8;
    private static final char SEPARATEUR = ';';
    private String numeroCompte;

    /**
     * Parse un fichier LCL
     */
    public List<Map<String, String>> parserFichier(String cheminFichier) {
        validationFichier(cheminFichier);
        List<Map<String, String>> donnees = new ArrayList<>();

        try (FileReader fileReader = new FileReader(cheminFichier);
             CSVReader csvReader = new CSVReaderBuilder(fileReader)
                     .withCSVParser(new CSVParserBuilder()
                             .withSeparator(SEPARATEUR)
                             .build())
                     .withSkipLines(0)
                     .build()) {

            List<String[]> toutesLesLignes = csvReader.readAll();

            if (toutesLesLignes.size() < 2) {
                throw new IllegalStateException("Le fichier LCL est invalide ou trop court");
            }

            // Extraire le numéro de compte de la première ligne


            // Créer les en-têtes par défaut pour LCL
            String[] entetes = creerEntetesParDefaut();

            // Traiter les lignes de données à partir de la ligne 2
            for (int i = LIGNE_PREMIERE_TRANSACTION; i < toutesLesLignes.size(); i++) {
                String[] ligne = toutesLesLignes.get(i);

                if (estLigneVide(ligne)) {
                    continue;
                }

                // Vérifier le nombre de colonnes
                if (ligne.length != NOMBRE_COLONNES_ATTENDUES) {
                    continue;  // Ignorer les lignes mal formatées
                }

                Map<String, String> enregistrement = new HashMap<>();

                for (int j = 0; j < entetes.length && j < ligne.length; j++) {
                    String cle = entetes[j];
                    String valeur = ligne[j] != null ? ligne[j].trim() : "";
                    enregistrement.put(cle, valeur);
                }

                donnees.add(enregistrement);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Erreur de lecture du fichier LCL : " + e.getMessage(), e);
        } catch (CsvException e) {
            throw new IllegalStateException("Erreur de parsing du fichier LCL : " + e.getMessage(), e);
        }

        return donnees;
    }

    /**
     * Crée les en-têtes par défaut pour LCL
     */
    private String[] creerEntetesParDefaut() {
        return new String[]{
                "Date",
                "Montant",
                "Mode de paiement",
                "Libellé",
                "Zero",
                "Divers",
                "Reference",
                "Supplément"
        };
    }


    /**
     * Vérifie la structure du fichier LCL
     */
    public boolean verifierStructure(String cheminFichier) {
        try (FileReader fileReader = new FileReader(cheminFichier);
             CSVReader csvReader = new CSVReaderBuilder(fileReader)
                     .withCSVParser(new CSVParserBuilder()
                             .withSeparator(SEPARATEUR)
                             .build())
                     .build()) {

            List<String[]> lignes = csvReader.readAll();

            if (lignes.size() < 2) {
                return false;
            }

            // Vérifier que la deuxième ligne a 8 colonnes
            String[] secondeLigne = lignes.get(LIGNE_PREMIERE_TRANSACTION);

            return secondeLigne.length == NOMBRE_COLONNES_ATTENDUES;

        } catch (Exception e) {
            return false;
        }
    }

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

    private void validationFichier(String cheminFichier) {
        Path path = Paths.get(cheminFichier);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Le fichier n'existe pas : " + cheminFichier);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Le fichier n'est pas accessible en lecture : " + cheminFichier);
        }
    }

    public String getNumeroCompte() {
        return numeroCompte;
    }

    /**
     * Extrait le numéro de compte de la première ligne
     */


}

