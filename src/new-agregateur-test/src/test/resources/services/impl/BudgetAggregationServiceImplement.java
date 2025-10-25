package com.agregateur.dimsoft.agregateur_production.services.impl;

import com.agregateur.dimsoft.agregateur_production.Enum.UserDecision;
import com.agregateur.dimsoft.agregateur_production.beans.Bank;
import com.agregateur.dimsoft.agregateur_production.beans.Budget;
import com.agregateur.dimsoft.agregateur_production.beans.Compte;
import com.agregateur.dimsoft.agregateur_production.factory.BudgetFactory;
import com.agregateur.dimsoft.agregateur_production.models.AgregationResultDto;
import com.agregateur.dimsoft.agregateur_production.models.TransactionCADto;
import com.agregateur.dimsoft.agregateur_production.models.TransactionLCLDto;
import com.agregateur.dimsoft.agregateur_production.repositories.BudgetRepository;
import com.agregateur.dimsoft.agregateur_production.services.BudgetAggregationService;
import com.agregateur.dimsoft.agregateur_production.services.UserInteractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BudgetAggregationServiceImplement implements BudgetAggregationService {

    private final BudgetRepository budgetRepository;
    private final BudgetFactory budgetFactory;
    private final UserInteractionService userInteractionService;


    public BudgetAggregationServiceImplement(
            BudgetRepository budgetRepository,
            BudgetFactory budgetFactory,
            UserInteractionService userInteractionService
    ) {
        this.budgetRepository = budgetRepository;
        this.budgetFactory = budgetFactory;
        this.userInteractionService = userInteractionService;
    }

    @Override
    @Transactional
    public AgregationResultDto aggregateCATransaction(List<TransactionCADto> transactions, Compte compte, Bank bank) {
//        log.info("Début agrégation de {} transactions CA pour compte {} et banque {}",
//                transactions.size(), compte.getId(), bank.getId());

        return processTransactions(transactions, compte, bank, "CA");

    }

    @Override
    @Transactional
    public AgregationResultDto aggregateLCLTransactions(List<TransactionLCLDto> transactions, Compte compte, Bank bank) {
//        log.info("Début agrégation de {} transactions LCL pour compte {} et banque {}",
//                transactions.size(), compte.getId(), bank.getId());

        return processTransactions(transactions, compte, bank, "LCL");

    }


    private AgregationResultDto processTransactions(
            List<?> transactions,
            Compte compte,
            Bank bank,
            String typeFichier
    ) {
        int enregistrees = 0;
        int ignorees = 0;
        UserDecision globalDecision = null;

        for (Object transaction : transactions) {
            Budget budgetToSave = createBudgetFromDTO(transaction, compte, bank, typeFichier);
            Optional<Budget> duplicate = budgetRepository.findDuplicateTransaction(
                    budgetToSave.getDateOperation(),
                    budgetToSave.getLibelle(),
                    Double.valueOf(budgetToSave.getMontant())
            );

            if (duplicate.isPresent()) {
                if (globalDecision == null) {
                    globalDecision = askUserForDecision(budgetToSave);
                }

                switch (globalDecision) {
                    case ANNULER_AGREGATION:
//                        log.info("Agrégation annulée par l'utilisateur");
                        userInteractionService.displayMessage(
                                "Vous avez choisi d'annuler l'enregistrement en cours. Aucune donnée ne sera ajoutée en base."
                        );
                        return AgregationResultDto.cancelled("Agrégation interrompue par l'utilisateur.");

                    case CONTINUER_AVEC_REDONDANCE:
                        budgetRepository.save(budgetToSave);
                        enregistrees++;
//                        log.debug("Transaction redondante enregistrée quand même.");
                        break;

                    case CONTINUER_SANS_REDONDANCE:
                        ignorees++;
//                        log.debug("Transaction redondante ignorée.");
                        break;
                }
            } else {
                budgetRepository.save(budgetToSave);
                enregistrees++;
//                log.debug("Nouvelle transaction enregistrée: {} - {}",
//                        budgetToSave.getDateOperation(), budgetToSave.getLibelle());
            }
        }

//        log.info("Agrégation terminée: {} enregistrées, {} ignorées", enregistrees, ignorees);
//        return buildResult(globalDecision, enregistrees, ignorees);
        return null;
    }


    private Budget createBudgetFromDTO(Object dto, Compte compte, Bank bank, String type) {
        if ("CA".equals(type)) {
            return budgetFactory.createFromCA((TransactionCADto) dto, compte, bank);
        } else {
            return budgetFactory.createFromLCL((TransactionLCLDto) dto, compte, bank);
        }
    }

    private UserDecision askUserForDecision(Budget budget) {
        String info = String.format(
                "Transaction redondante détectée :\nDate : %s\nLibellé : %s\nMontant : %.2f€",
                budget.getDateOperation(),
                budget.getLibelle(),
                budget.getMontant()
        );

        return userInteractionService.askUserDecision(info);
    }

    private AgregationResultDto buildResult(UserDecision decision, int enregistrees, int ignorees) {
        if (decision == null) {
            return AgregationResultDto.success(
                    "Copie terminée avec succès.",
                    enregistrees
            );
        } else if (decision == UserDecision.CONTINUER_AVEC_REDONDANCE) {
            return AgregationResultDto.success(
                    "Copie avec redondances terminée avec succès.",
                    enregistrees
            );
        } else {
            return AgregationResultDto.successWithSkipped(
                    "Copie sans redondances terminée avec succès.",
                    enregistrees,
                    ignorees
            );
        }
    }
}
