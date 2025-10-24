package com.agregateur.dimsoft.agregateur_production.services.impl;

import com.agregateur.dimsoft.agregateur_production.TypeFichierBancaire;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;


@Component
public class DetecteurTypeFichierBancaire {

    /**
     * Détecte le type de fichier bancaire en fonction de son nom
     */
    public static TypeFichierBancaire detecterType(String nomFichier) {
        if (nomFichier == null || nomFichier.isEmpty()) {
            return TypeFichierBancaire.INCONNU;
        }

        // Convertir en majuscules pour la comparaison (case-insensitive)
        String nom = nomFichier.toUpperCase();

        // Vérifier si c'est un fichier CSV
        if (!nom.endsWith(".CSV")) {
            return TypeFichierBancaire.INCONNU;
        }

        // Reconnaître CA : commence par "CA" suivi de chiffres
        if (nom.matches("^CA\\d+.*\\.CSV$")) {
            return TypeFichierBancaire.CA;
        }

        // Reconnaître LCL : commence par "T_CPTE_"
        if (nom.contains("T_CPTE_") || nom.startsWith("T_CPTE_")) {
            return TypeFichierBancaire.LCL;
        }

        return TypeFichierBancaire.INCONNU;
    }

    /**
     * Détecte le type en fonction du chemin complet
     */
    public static TypeFichierBancaire detecterTypeDepuisChemin(String cheminComplet) {
        if (cheminComplet == null || cheminComplet.isEmpty()) {
            return TypeFichierBancaire.INCONNU;
        }

        Path path = Paths.get(cheminComplet);
        return detecterType(path.getFileName().toString());
    }

    /**
     * Vérifie si le fichier est de type CA
     */
    public static boolean estFichierCA(String nomFichier) {
        return detecterType(nomFichier) == TypeFichierBancaire.CA;
    }

    /**
     * Vérifie si le fichier est de type LCL
     */
    public static boolean estFichierLCL(String nomFichier) {
        return detecterType(nomFichier) == TypeFichierBancaire.LCL;
    }

    /**
     * Vérifie si le fichier est d'une banque reconnue
     */
    public static boolean estFichierValide(String nomFichier) {
        TypeFichierBancaire type = detecterType(nomFichier);
        return type != TypeFichierBancaire.INCONNU;
    }
}
