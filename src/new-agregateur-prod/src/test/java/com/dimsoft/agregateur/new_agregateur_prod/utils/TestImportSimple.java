package com.dimsoft.agregateur.new_agregateur_prod.utils;

import com.dimsoft.agregateur.new_agregateur_prod.factory.BudgetFactory;
import com.dimsoft.agregateur.new_agregateur_prod.models.AggregationResultDto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionCADto;
import com.dimsoft.agregateur.new_agregateur_prod.services.impl.BudgetServiceImportImplement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test simple pour vérifier que l'import fonctionne
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestImportSimple {

    private  final BudgetServiceImportImplement budgetService;

    public TestImportSimple(BudgetServiceImportImplement budgetService) {
        this.budgetService = budgetService;
    }

    @Test
    public void testImportCA_SansDoublon() {
        System.out.println("\n========== TEST IMPORT CA SANS DOUBLON ==========\n");

        // 1. Créer des transactions de test
        List<TransactionCADto> transactions = new ArrayList<>();

        // Transaction 1 : DÉBIT
        transactions.add(BudgetFactory.creerTransactionCA(
                "12/08/2025",
                "CHEQUE EMIS 8186609",
                150.0,  // débit
                null    // pas de crédit
        ));

        // Transaction 2 : CRÉDIT
        transactions.add(BudgetFactory.creerTransactionCA(
                "08/08/2025",
                "VIREMENT SALAIRE",
                null,    // pas de débit
                1100.0   // crédit
        ));

        // 2. Appeler le service
        // Compte ID = 1, Bank ID = 1 (doivent exister en BD)
        AggregationResultDto resultat = budgetService.importCATransactions(
                transactions,
                1L,  // compteId
                1L   // bankId
        );

        // 3. Vérifications
        System.out.println("\n📊 RÉSULTAT :");
        System.out.println("  - Message : " + resultat.getMessage());
        System.out.println("  - Enregistrées : " + resultat.getSavedTransactions());
        System.out.println("  - Doublons : " + resultat.getDuplicateTransactions());
        System.out.println("  - Succès : " + resultat.isSuccess());

        assertTrue("L'opération doit être un succès", resultat.isSuccess());
        assertEquals("2 transactions doivent être enregistrées", 2, resultat.getSavedTransactions());
        assertEquals("Aucun doublon attendu", 0, resultat.getDuplicateTransactions());
        assertEquals("Message attendu", "la copie s'est parfaitement deroulée", resultat.getMessage());

        System.out.println("\n✅ TEST RÉUSSI !\n");
    }

    @Test
    public void testImportLCL_SansDoublon() {
        System.out.println("\n========== TEST IMPORT LCL SANS DOUBLON ==========\n");

        // 1. Créer des transactions LCL
        List<com.dimsoft.agregateur.new_agregateur_prod.models.TransactionLCLDto> transactions = new ArrayList<>();

        transactions.add(BudgetFactory.creerTransactionLCL(
                "22/05/2025",
                "Carte CB DR TEIXEIRA",
                -23.04
        ));

        transactions.add(BudgetFactory.creerTransactionLCL(
                "22/05/2025",
                "Carte CB MISTIGRIFF",
                -4.99
        ));

        // 2. Appeler le service
        AggregationResultDto resultat = budgetService.importLCLTransactions(
                transactions,
                2L,  // compteId
                2L   // bankId
        );

        // 3. Vérifications
        System.out.println("\n📊 RÉSULTAT :");
        System.out.println("  - Message : " + resultat.getMessage());
        System.out.println("  - Enregistrées : " + resultat.getSavedTransactions());

        assertTrue(resultat.isSuccess());
        assertEquals(2, resultat.getSavedTransactions());

        System.out.println("\n✅ TEST LCL RÉUSSI !\n");
    }
}