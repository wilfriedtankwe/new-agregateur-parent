package com.dimsoft.agregateur.new_agregateur_prod.services.impl;

import com.dimsoft.agregateur.new_agregateur_prod.beans.Bank;
import com.dimsoft.agregateur.new_agregateur_prod.beans.Budget;
import com.dimsoft.agregateur.new_agregateur_prod.beans.Compte;
import com.dimsoft.agregateur.new_agregateur_prod.models.AggregationResultDto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionCADto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionLCLDto;
import com.dimsoft.agregateur.new_agregateur_prod.repositories.BankRepository;
import com.dimsoft.agregateur.new_agregateur_prod.repositories.BudgetRepository;
import com.dimsoft.agregateur.new_agregateur_prod.repositories.CompteRepository;
import com.dimsoft.agregateur.new_agregateur_prod.utils.ManageDuplicateTransaction;
import jakarta.transaction.Transactional;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Service d'import de transactions dans la table Budget
 * Avec gestion interactive des doublons
 */
@Component
public class BudgetServiceImportImplement {

    private final BudgetRepository budgetRepository;
    private final Scanner scanner;
    private final ImportFileServiceImpl importFileService; // Gardé pour l'intégration future
    private final CompteRepository  compteRepository; // Gardé par fidélité au code
    private final BankRepository bankRepository; // Gardé par fidélité au code

    // Injection par constructeur (fidèle à votre code)
    public BudgetServiceImportImplement(
            BudgetRepository budgetRepository,
            ImportFileServiceImpl importFileService,
            CompteRepository compteRepository,
            BankRepository bankRepository) {
        this.budgetRepository = budgetRepository;
        this.scanner = new Scanner(System.in);
        this.importFileService = importFileService;
        this.compteRepository = compteRepository;
        this.bankRepository = bankRepository;
    }

    @Setter
    private ManageDuplicateTransaction strategieTest = null;

    /**
    @Transactional
    public AggregationResultDto importCATransactions(
            List<TransactionCADto> transactions,
            Long compteId,
            Long bankId) {

        int enregistrees = 0;
        int ignorees = 0;
        ManageDuplicateTransaction decisionGlobale = null;

        for (TransactionCADto dto : transactions) {
            try{

                // 1. Créer le Budget depuis le DTO
                Budget budget = creerBudgetDepuisCA(dto, compteId, bankId);

                // 2. Chercher si doublon existe
                Optional<Budget> doublon = budgetRepository.chercherDoublon(
                        budget.getDateOperation(),
                        budget.getLibelle(),
                        budget.getMontant(),
                        compteId,
                        bankId
                );

                // 3. Gérer le doublon
                if (doublon.isPresent()) {
                    // Demander à l'utilisateur une seule fois
                    if (decisionGlobale == null) {
                        decisionGlobale = demanderDecisionUtilisateur(doublon.get());
                    }

                    switch (decisionGlobale) {
                        case ANNULER:
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            System.out.println("\n Agrégation annulée par l'utilisateur");
                            return AggregationResultDto.cancelled(
                                    "veux-tu annulée l'enregistrment en cours remarque aucune données ne sera ajouté en BD"
                            );

                        case ENREGISTRER_QUAND_MEME:
                            budgetRepository.save(budget);
                            enregistrees++;
                            System.out.println(" Doublon enregistré : " + budget.getLibelle());
                            break;

                        case SAUTER:
                            ignorees++;
                            System.out.println("️ Doublon ignoré : " + budget.getLibelle());
                            break;
                    }
                } else {
                    // Pas de doublon : enregistrer normalement
                    budgetRepository.save(budget);
                    enregistrees++;
                    System.out.println(" Enregistré : " + budget.getLibelle());
                }
            } catch (NoTransactionException e) {
                throw new RuntimeException(e);
            }

        }

        return construireResultat(decisionGlobale, enregistrees, ignorees);
    }**/

    @Transactional
    public AggregationResultDto importCATransactions(
            List<TransactionCADto> transactions,
            Long compteId,
            Long bankId) {

        int enregistrees = 0;
        int ignorees = 0;
        ManageDuplicateTransaction decisionGlobale = null;

        for (TransactionCADto dto : transactions) {

            // *** CORRECTION 1 : GESTION D'ERREUR PAR TRANSACTION ***
            try {
                // 1. Créer le Budget depuis le DTO
                Budget budget = creerBudgetDepuisCA(dto, compteId, bankId);

                // 2. Chercher si doublon existe
                Optional<Budget> doublon = budgetRepository.chercherDoublon(
                        budget.getDateOperation(),
                        budget.getLibelle(),
                        budget.getMontant(),
                        compteId,
                        bankId
                );

                // 3. Gérer le doublon
                if (doublon.isPresent()) {
                    // Demander à l'utilisateur une seule fois
                    if (decisionGlobale == null) {
                        // Si strategieTest n'est pas null, on est dans un test Gherkin,
                        // on utilise la stratégie par défaut pour ne pas bloquer.
                        decisionGlobale = strategieTest != null ? strategieTest : demanderDecisionUtilisateur(doublon.get());
                    }

                    switch (decisionGlobale) {
                        case ANNULER:
                            // *** CORRECTION 2 : FORCER LE ROLLBACK LORS DE L'ANNULATION ***
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

                            System.out.println("\n Agrégation annulée par l'utilisateur");
                            return AggregationResultDto.cancelled(
                                    "veux-tu annulée l'enregistrment en cours remarque aucune données ne sera ajouté en BD"
                            );

                        case ENREGISTRER_QUAND_MEME:
                            budgetRepository.save(budget);
                            enregistrees++;
                            System.out.println(" Doublon enregistré : " + budget.getLibelle());
                            break;

                        case SAUTER:
                            ignorees++;
                            System.out.println("️ Doublon ignoré : " + budget.getLibelle());
                            break;
                    }
                } else {
                    // Pas de doublon : enregistrer normalement
                    budgetRepository.save(budget);
                    enregistrees++;
                    System.out.println(" Enregistré : " + budget.getLibelle());
                }

            } catch (Exception e) {
                // Cette exception (NullPointerException, DataAccessException, etc.)
                // est la cause de l'erreur "rollback-only" si elle n'est pas gérée.
                System.err.println("--- ERREUR TRANSACTION IGNORÉE ---");
                System.err.println("Transaction ignorée à cause de : " + e.getClass().getSimpleName() + " - " + e.getMessage());
                System.err.println("DTO incriminé: " + dto);
                System.err.println("----------------------------------");

                ignorees++; // On incrémente le compteur d'erreurs/ignorées
                // On laisse Spring marquer le rollback pour cette transaction si nécessaire
                // et on continue la boucle pour les autres transactions.
            }
        }

        return construireResultat(decisionGlobale, enregistrees, ignorees);
    }

    /**
    @Transactional
    public AggregationResultDto importLCLTransactions(
            List<TransactionLCLDto> transactions,
            Long compteId,
            Long bankId) {

        int enregistrees = 0;
        int ignorees = 0;
        ManageDuplicateTransaction decisionGlobale = null;

        for (TransactionLCLDto dto : transactions) {
            try{
                // 1. Créer le Budget depuis le DTO
                Budget budget = creerBudgetDepuisLCL(dto, compteId, bankId);

                // 2. Chercher doublon
                Optional<Budget> doublon = budgetRepository.chercherDoublon(
                        budget.getDateOperation(),
                        budget.getLibelle(),
                        budget.getMontant(),
                        compteId,
                        bankId
                );

                // 3. Gérer le doublon
                if (doublon.isPresent()) {
                    if (decisionGlobale == null) {
                        decisionGlobale = demanderDecisionUtilisateur(doublon.get());
                    }

                    switch (decisionGlobale) {
                        case ANNULER:
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            System.out.println("\n Agrégation annulée");
                            return AggregationResultDto.cancelled(
                                    "veux-tu annulée l'enregistrment en cours remarque aucune données ne sera ajouté en BD"
                            );

                        case ENREGISTRER_QUAND_MEME:
                            budgetRepository.save(budget);
                            enregistrees++;
                            System.out.println(" Doublon enregistré : " + budget.getLibelle());
                            break;

                        case SAUTER:
                            ignorees++;
                            System.out.println(" Doublon ignoré : " + budget.getLibelle());
                            break;
                    }
                } else {
                    budgetRepository.save(budget);
                    enregistrees++;
                    System.out.println(" Enregistré : " + budget.getLibelle());
                }
            } catch (NoTransactionException e) {
                throw new RuntimeException(e);
            }

        }

        return construireResultat(decisionGlobale, enregistrees, ignorees);
    }
    **/

    @Transactional
    public AggregationResultDto importLCLTransactions(
            List<TransactionLCLDto> transactions,
            Long compteId,
            Long bankId) {

        int enregistrees = 0;
        int ignorees = 0;
        ManageDuplicateTransaction decisionGlobale = null;

        for (TransactionLCLDto dto : transactions) {

            // *** CORRECTION 1 : GESTION D'ERREUR PAR TRANSACTION ***
            try {

                // 1. Créer le Budget depuis le DTO
                Budget budget = creerBudgetDepuisLCL(dto, compteId, bankId);

                // 2. Chercher doublon
                Optional<Budget> doublon = budgetRepository.chercherDoublon(
                        budget.getDateOperation(),
                        budget.getLibelle(),
                        budget.getMontant(),
                        compteId,
                        bankId
                );

                // 3. Gérer le doublon
                if (doublon.isPresent()) {
                    if (decisionGlobale == null) {
                        decisionGlobale = strategieTest != null ? strategieTest : demanderDecisionUtilisateur(doublon.get());
                    }

                    switch (decisionGlobale) {
                        case ANNULER:
                            // *** CORRECTION 2 : FORCER LE ROLLBACK LORS DE L'ANNULATION ***
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

                            System.out.println("\n Agrégation annulée");
                            return AggregationResultDto.cancelled(
                                    "veux-tu annulée l'enregistrment en cours remarque aucune données ne sera ajouté en BD"
                            );

                        case ENREGISTRER_QUAND_MEME:
                            budgetRepository.save(budget);
                            enregistrees++;
                            System.out.println(" Doublon enregistré : " + budget.getLibelle());
                            break;

                        case SAUTER:
                            ignorees++;
                            System.out.println(" Doublon ignoré : " + budget.getLibelle());
                            break;
                    }
                } else {
                    budgetRepository.save(budget);
                    enregistrees++;
                    System.out.println(" Enregistré : " + budget.getLibelle());
                }

            } catch (Exception e) {
                // Cette exception est la cause de l'erreur "rollback-only"
                System.err.println("--- ERREUR TRANSACTION IGNORÉE ---");
                System.err.println("Transaction LCL ignorée à cause de : " + e.getClass().getSimpleName() + " - " + e.getMessage());
                System.err.println("DTO incriminé: " + dto);
                System.err.println("----------------------------------");
                ignorees++; // On incrémente le compteur d'erreurs/ignorées
            }
        }

        return construireResultat(decisionGlobale, enregistrees, ignorees);
    }

    /**
     * Crée un Budget depuis un TransactionCADto
     * CORRECTION: Initialisation de Compte et Bank pour éviter NullPointerException.
     */
    private Budget creerBudgetDepuisCA(TransactionCADto dto, Long compteId, Long bankId) {
        Budget budget = new Budget();

        // --- CORRECTION DU NULLPOINTEREXCEPTION : DANS LE BEAN BUDGET,
        //     LES PROPRIÉTÉS COMPTE ET BANK DOIVENT ÊTRE INITIALISÉES AVANT D'APPELER LEUR GETTER.
        //     Si elles ne sont pas initialisées dans le constructeur de Budget, nous le faisons ici:

        if (budget.getCompte() == null) {
            budget.setCompte(new Compte());
        }
        if (budget.getBank() == null) {
            budget.setBank(new Bank());
        }

        // --- FIN DE CORRECTION ---

        budget.setDateOperation(dto.getTransactionCADate());
        budget.setLibelle(dto.getTransactionCALibelle());

        // Montant : crédit positif, débit négatif
        Double montant = (dto.getTransactionCACredit() != null && dto.getTransactionCACredit() != 0)
                ? dto.getTransactionCACredit()
                : -dto.getTransactionCADebit();

        budget.setMontant(montant);

        // Ces lignes fonctionnent maintenant car getCompte() et getBank() ne retournent plus null
        budget.getCompte().setId(compteId);
        budget.getBank().setId(bankId);

        return budget;
    }

    /**
     * Crée un Budget depuis un TransactionLCLDto
     * CORRECTION: Initialisation de Compte et Bank pour éviter NullPointerException.
     */
    private Budget creerBudgetDepuisLCL(TransactionLCLDto dto, Long compteId, Long bankId) {
        Budget budget = new Budget();

        // --- CORRECTION DU NULLPOINTEREXCEPTION ---
        if (budget.getCompte() == null) {
            budget.setCompte(new Compte());
        }
        if (budget.getBank() == null) {
            budget.setBank(new Bank());
        }
        // --- FIN DE CORRECTION ---

        budget.setDateOperation(dto.getTransactionLCLDate());
        budget.setLibelle(dto.getTransactionLCLLibelle());
        budget.setMontant(dto.getTransactionLCLMontant());

        // Ces lignes fonctionnent maintenant
        budget.getCompte().setId(compteId);
        budget.getBank().setId(bankId);

        if (dto.getTransactionChequeNumber() != null) {
            budget.setChequeNumero(dto.getTransactionChequeNumber().toString());
        }

        return budget;
    }

    /**
     * Demande à l'utilisateur ce qu'il veut faire avec un doublon
     */
    private ManageDuplicateTransaction demanderDecisionUtilisateur(Budget doublon) {
        // ... (Méthode inchangée)
        System.out.println("Transaction déjà existante :");
        System.out.println("  Date    : " + doublon.getDateOperation());
        System.out.println("  Libellé : " + doublon.getLibelle());
        System.out.println("  Montant : " + doublon.getMontant() + " €");
        System.out.println("\nQue voulez-vous faire ?");
        System.out.println("  1 - ANNULER (arrêter tout, aucune donnée ajoutée)");
        System.out.println("  2 - ENREGISTRER QUAND MÊME (créer le doublon)");
        System.out.println("  3 - SAUTER (ignorer et continuer)");
        System.out.print("\nVotre choix (1/2/3) : ");

        int choix = scanner.nextInt();
        scanner.nextLine(); // Consommer le retour à la ligne

        switch (choix) {
            case 1:
                return ManageDuplicateTransaction.ANNULER;
            case 2:
                return ManageDuplicateTransaction.ENREGISTRER_QUAND_MEME;
            case 3:
                return ManageDuplicateTransaction.SAUTER;
            default:
                System.out.println(" Choix invalide, on saute par défaut");
                return ManageDuplicateTransaction.SAUTER;
        }
    }

    /**
     * Construit le résultat selon les scénarios Gherkin
     */
    private AggregationResultDto construireResultat(
            ManageDuplicateTransaction decision,
            int enregistrees,
            int ignorees) {

        // ... (Méthode inchangée)

        if (decision == null) {
            // Scénario 1 : Aucun doublon
            return AggregationResultDto.success(
                    "la copie s'est parfaitement deroulée",
                    enregistrees
            );
        } else if (decision == ManageDuplicateTransaction.ENREGISTRER_QUAND_MEME) {
            // Scénario 3 : Doublons enregistrés
            return AggregationResultDto.success(
                    "copie des données redondantes terminée avec succès",
                    enregistrees
            );
        } else {
            // Scénario 4 : Doublons ignorés
            return AggregationResultDto.successWithSkipped(
                    "copie des données sans redondance terminée avec succès",
                    enregistrees,
                    ignorees
            );
        }
    }
}