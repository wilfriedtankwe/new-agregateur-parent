package com.dimsoft.agregateur.new_agregateur_prod.services;

import com.dimsoft.agregateur.new_agregateur_prod.components.DetecteurTypeFichierBancaire;
import com.dimsoft.agregateur.new_agregateur_prod.components.ParseurCAFile;
import com.dimsoft.agregateur.new_agregateur_prod.components.ParseurLLCFile;
import com.dimsoft.agregateur.new_agregateur_prod.enums.TypeFichierBancaire;
import com.dimsoft.agregateur.new_agregateur_prod.models.AggregationResultDto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionCADto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionLCLDto;
import com.dimsoft.agregateur.new_agregateur_prod.services.impl.BudgetServiceImportImplement;
import com.dimsoft.agregateur.new_agregateur_prod.services.impl.ImportFileServiceImpl;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service principal d'agrégation des transactions bancaires
 * Orchestre tous les composants : détection, parsing, extraction, et import
 */
@Service
@Transactional
@Getter
public class AggregationService {

    private final ParseurCAFile parseurCAFile;
    private final ParseurLLCFile parseurLLCFile;
    private final ImportFileServiceImpl importFileService;
    private final BudgetServiceImportImplement budgetServiceImport;

    // Chemin du répertoire contenant les fichiers
    private static final String REPERTOIRE_FICHIERS = "data/fichiers";

    // Formatters de dates pour CA et LCL
    private static final DateTimeFormatter DATE_FORMATTER_CA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_LCL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AggregationService(
            ParseurCAFile parseurCAFile,
            ParseurLLCFile parseurLLCFile,
            ImportFileServiceImpl importFileService,
            BudgetServiceImportImplement budgetServiceImport) {
        this.parseurCAFile = parseurCAFile;
        this.parseurLLCFile = parseurLLCFile;
        this.importFileService = importFileService;
        this.budgetServiceImport = budgetServiceImport;
    }

    /**
     * Méthode principale d'agrégation - Point d'entrée du service
     *
     */
    public AggregationGlobaleResultDto demarrerAggregation() {
        System.out.println("=== DÉBUT DE L'AGRÉGATION DES TRANSACTIONS BANCAIRES ===\n");

        AggregationGlobaleResultDto resultatGlobal = new AggregationGlobaleResultDto();

        try {
            // Étape 1 : Récupérer tous les fichiers du répertoire
            List<File> fichiers = recupererFichiersDuRepertoire();
            System.out.println("✓ " + fichiers.size() + " fichier(s) trouvé(s) dans le répertoire\n");

            if (fichiers.isEmpty()) {
                resultatGlobal.setStatut("ECHEC");
                resultatGlobal.setMessage("Aucun fichier trouvé dans le répertoire " + REPERTOIRE_FICHIERS);
                return resultatGlobal;
            }

            // Étape 2 : Extraire les numéros de compte et bank_id de tous les fichiers
            System.out.println("--- Extraction des numéros de compte ---");
            Map<String, ImportFileServiceImpl.AccountResult> comptes = importFileService.extractNumbersFromFiles();
            System.out.println("✓ Numéros de compte extraits pour " + comptes.size() + " fichier(s)\n");

            // Étape 3 : Traiter chaque fichier individuellement
            for (File fichier : fichiers) {
                traiterUnFichier(fichier, comptes, resultatGlobal);
            }

            // Étape 4 : Construire le résultat final
            resultatGlobal.calculerStatutFinal();
            System.out.println("\n=== FIN DE L'AGRÉGATION ===");
            System.out.println("Fichiers traités : " + resultatGlobal.getFichiersTraites());
            System.out.println("Transactions importées : " + resultatGlobal.getTransactionsImportees());
            System.out.println("Erreurs : " + resultatGlobal.getErreurs());

        } catch (Exception e) {
            resultatGlobal.setStatut("ECHEC");
            resultatGlobal.setMessage("Erreur globale : " + e.getMessage());
            System.err.println("❌ Erreur fatale : " + e.getMessage());
            e.printStackTrace();
        }

        return resultatGlobal;
    }

    /**
     * Traite un fichier unique : détection, parsing, et import
     */
    private void traiterUnFichier(
            File fichier,
            Map<String, ImportFileServiceImpl.AccountResult> comptes,
            AggregationGlobaleResultDto resultatGlobal) {

        String nomFichier = fichier.getName();
        String cheminFichier = fichier.getAbsolutePath();

        System.out.println("→ Traitement du fichier : " + nomFichier);

        try {
            // Étape 1 : Détecter le type de fichier
            TypeFichierBancaire typeFichier = DetecteurTypeFichierBancaire.detecterType(nomFichier);
            System.out.println("  Type détecté : " + typeFichier);

            if (typeFichier == TypeFichierBancaire.INCONNU) {
                System.out.println("  ⚠ Type inconnu, fichier ignoré\n");
                resultatGlobal.ajouterErreur(nomFichier, "Type de fichier non reconnu");
                return;
            }

            // Étape 2 : Récupérer le compte et bank_id associés
            ImportFileServiceImpl.AccountResult accountResult = comptes.get(nomFichier);
            if (accountResult == null) {
                System.out.println("  ⚠ Aucun compte trouvé pour ce fichier\n");
                resultatGlobal.ajouterErreur(nomFichier, "Numéro de compte non trouvé");
                return;
            }

            String bankId = accountResult.getBankId();
            if (bankId == null || bankId.startsWith("Aucun") || bankId.startsWith("Erreur")) {
                System.out.println("  ⚠ Bank ID invalide : " + bankId + "\n");
                resultatGlobal.ajouterErreur(nomFichier, "Bank ID non trouvé en base de données");
                return;
            }

            Long bankIdLong = Long.parseLong(bankId);
            System.out.println("  Compte : " + accountResult.getFormattedAccount() + " | Bank ID : " + bankId);

            // Étape 3 : Traiter selon le type de fichier
            if (typeFichier == TypeFichierBancaire.CA) {
                traiterFichierCA(cheminFichier, nomFichier, bankIdLong, resultatGlobal);
            } else if (typeFichier == TypeFichierBancaire.LCL) {
                traiterFichierLCL(cheminFichier, nomFichier, bankIdLong, resultatGlobal);
            }

        } catch (Exception e) {
            System.err.println("  ❌ Erreur lors du traitement : " + e.getMessage() + "\n");
            resultatGlobal.ajouterErreur(nomFichier, e.getMessage());
        }
    }

    /**
     * Traite un fichier Crédit Agricole
     */
    private void traiterFichierCA(
            String cheminFichier,
            String nomFichier,
            Long bankId,
            AggregationGlobaleResultDto resultatGlobal) {

        System.out.println("  → Traitement fichier CA");

        // Étape 1 : Vérifier la structure
        boolean structureValide = parseurCAFile.verifierStructure(cheminFichier);
        if (!structureValide) {
            System.out.println("  ❌ Structure invalide\n");
            resultatGlobal.ajouterErreur(nomFichier, "Structure de fichier CA invalide");
            return;
        }
        System.out.println("  ✓ Structure validée");

        // Étape 2 : Parser le fichier
        List<Map<String, String>> donneesCA = parseurCAFile.parserFichier(cheminFichier);
        System.out.println("  ✓ " + donneesCA.size() + " transactions parsées");

        // Étape 3 : Convertir en DTO
        List<TransactionCADto> transactionsCA = convertirVersTransactionCADto(donneesCA);

        // Étape 4 : Importer dans Budget
        // Note : On utilise bankId comme compteId pour l'instant (à ajuster selon votre logique métier)
        AggregationResultDto resultat = budgetServiceImport.importCATransactions(
                transactionsCA,
                bankId, // compteId
                bankId  // bankId
        );

        System.out.println("  ✓ Import terminé : " + resultat.getMessage());
        resultatGlobal.ajouterResultat(nomFichier, resultat);
    }

    /**
     * Traite un fichier LCL
     */
    private void traiterFichierLCL(
            String cheminFichier,
            String nomFichier,
            Long bankId,
            AggregationGlobaleResultDto resultatGlobal) {

        System.out.println("  → Traitement fichier LCL");

        // Étape 1 : Vérifier la structure
        boolean structureValide = parseurLLCFile.verifierStructure(cheminFichier);
        if (!structureValide) {
            System.out.println("  ❌ Structure invalide\n");
            resultatGlobal.ajouterErreur(nomFichier, "Structure de fichier LCL invalide");
            return;
        }
        System.out.println("  ✓ Structure validée");

        // Étape 2 : Parser le fichier
        List<Map<String, String>> donneesLCL = parseurLLCFile.parserFichier(cheminFichier);
        System.out.println("  ✓ " + donneesLCL.size() + " transactions parsées");

        // Étape 3 : Convertir en DTO
        List<TransactionLCLDto> transactionsLCL = convertirVersTransactionLCLDto(donneesLCL);

        // Étape 4 : Importer dans Budget
        AggregationResultDto resultat = budgetServiceImport.importLCLTransactions(
                transactionsLCL,
                bankId, // compteId
                bankId  // bankId
        );

        System.out.println("  ✓ Import terminé : " + resultat.getMessage());
        resultatGlobal.ajouterResultat(nomFichier, resultat);
    }

    /**
     * Récupère tous les fichiers CSV du répertoire
     */
    private List<File> recupererFichiersDuRepertoire() {
        List<File> fichiers = new ArrayList<>();

        try {
            Path repertoire = Paths.get(REPERTOIRE_FICHIERS);

            if (!Files.exists(repertoire)) {
                System.err.println("❌ Le répertoire n'existe pas : " + REPERTOIRE_FICHIERS);
                return fichiers;
            }

            File dir = repertoire.toFile();
            File[] tousLesFichiers = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));

            if (tousLesFichiers != null) {
                for (File f : tousLesFichiers) {
                    fichiers.add(f);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des fichiers : " + e.getMessage());
        }

        return fichiers;
    }

    /**
     * Convertit les données CA en TransactionCADto
     */
    private List<TransactionCADto> convertirVersTransactionCADto(List<Map<String, String>> donnees) {
        List<TransactionCADto> transactions = new ArrayList<>();

        for (Map<String, String> ligne : donnees) {
            try {
                TransactionCADto dto = new TransactionCADto();

                // Date
                String dateStr = ligne.get("date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    dto.setTransactionCADate(parseDate(dateStr, DATE_FORMATTER_CA));
                }

                // Libellé
                dto.setTransactionCALibelle(ligne.getOrDefault("libelle", ""));

                // Débit
                String debitStr = ligne.get("debit");
                dto.setTransactionCADebit(parseDouble(debitStr));

                // Crédit
                String creditStr = ligne.get("credit");
                dto.setTransactionCACredit(parseDouble(creditStr));

                transactions.add(dto);

            } catch (Exception e) {
                System.err.println("  ⚠ Erreur conversion ligne CA : " + e.getMessage());
            }
        }

        return transactions;
    }

    /**
     * Convertit les données LCL en TransactionLCLDto
     */
    private List<TransactionLCLDto> convertirVersTransactionLCLDto(List<Map<String, String>> donnees) {
        List<TransactionLCLDto> transactions = new ArrayList<>();

        for (Map<String, String> ligne : donnees) {
            try {
                TransactionLCLDto dto = new TransactionLCLDto();

                // Date
                String dateStr = ligne.get("Date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    dto.setTransactionLCLDate(parseDate(dateStr, DATE_FORMATTER_LCL));
                }

                // Libellé
                dto.setTransactionLCLLibelle(ligne.getOrDefault("Libellé", ""));

                // Montant
                String montantStr = ligne.get("Montant");
                dto.setTransactionLCLMontant(parseDouble(montantStr));

                // Numéro de chèque (optionnel)
                String refStr = ligne.get("Reference");
                if (refStr != null && !refStr.isEmpty()) {
                    try {
                        dto.setTransactionChequeNumber((long) Integer.parseInt(refStr.trim()));
                    } catch (NumberFormatException ignored) {
                        // Ignorer si ce n'est pas un nombre
                    }
                }

                transactions.add(dto);

            } catch (Exception e) {
                System.err.println("  ⚠ Erreur conversion ligne LCL : " + e.getMessage());
            }
        }

        return transactions;
    }

    /**
     * Parse une date avec gestion d'erreur
     */
    private Date parseDate(String dateStr, DateTimeFormatter formatter) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            LocalDate localDate = LocalDate.parse(dateStr.trim(), formatter);
            // Convertir LocalDate en Date (java.util.Date)
            return java.sql.Date.valueOf(localDate);
        } catch (DateTimeParseException e) {
            System.err.println("  ⚠ Erreur parsing date : " + dateStr);
            return null;
        }
    }

    /**
     * Parse un double avec gestion d'erreur
     */
    private Double parseDouble(String valeur) {
        if (valeur == null || valeur.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Remplacer virgule par point et supprimer espaces
            String normalized = valeur.trim()
                    .replace(",", ".")
                    .replace(" ", "")
                    .replace("\u00A0", ""); // espace insécable

            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            System.err.println("  ⚠ Erreur parsing nombre : " + valeur);
            return 0.0;
        }
    }

    /**
     * DTO pour le résultat global de l'agrégation
     */
    public static class AggregationGlobaleResultDto {
        @Setter
        private String statut;
        @Setter
        private String message;
        @Getter
        private int fichiersTraites;
        @Getter
        private int transactionsImportees;
        @Getter
        private int transactionsIgnorees;
        @Getter
        private int erreurs;
        @Getter
        private List<String> detailsErreurs;

        public AggregationGlobaleResultDto() {
            this.fichiersTraites = 0;
            this.transactionsImportees = 0;
            this.transactionsIgnorees = 0;
            this.erreurs = 0;
            this.detailsErreurs = new ArrayList<>();
        }

        public void ajouterResultat(String nomFichier, AggregationResultDto resultat) {
            this.fichiersTraites++;
            this.transactionsImportees += resultat.getSavedTransactions();
            this.transactionsIgnorees += resultat.getDuplicateTransactions();
        }

        public void ajouterErreur(String nomFichier, String messageErreur) {
            this.erreurs++;
            this.detailsErreurs.add(nomFichier + " : " + messageErreur);
        }

        public void calculerStatutFinal() {
            if (erreurs == 0 && transactionsImportees > 0) {
                this.statut = "SUCCES";
                this.message = "Agrégation réussie : " + transactionsImportees + " transaction(s) importée(s)";
            } else if (erreurs > 0 && transactionsImportees > 0) {
                this.statut = "PARTIEL";
                this.message = "Agrégation partielle : " + transactionsImportees + " importée(s), " + erreurs + " erreur(s)";
            } else if (erreurs > 0) {
                this.statut = "ECHEC";
                this.message = "Échec de l'agrégation : " + erreurs + " erreur(s)";
            } else {
                this.statut = "VIDE";
                this.message = "Aucune transaction à importer";
            }
        }

        // Getters et Setters


    }
}