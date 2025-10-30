package com.dimsoft.agregateur.new_agregateur_prod.factory;

import com.dimsoft.agregateur.new_agregateur_prod.beans.Budget;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionCADto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionLCLDto;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;


@Component
public class BudgetFactory {
    private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    // ========== CRÉER UN BUDGET ==========
    public static Budget creerBudget(String date, String libelle, Float montant) {
        Budget b = new Budget();
        b.setDateOperation(parseDate(date));
        b.setLibelle(libelle);
        b.setMontant(Double.valueOf(montant));
        return b;
    }

    // ========== CRÉER UN DTO CA (CRÉDIT AGRICOLE) ==========
    public static TransactionCADto creerTransactionCA(String date, String libelle, Double debit, Double credit) {
        TransactionCADto dto = new TransactionCADto();
        dto.setTransactionCADate(parseDate(date));
        dto.setTransactionCALibelle(libelle);
        dto.setTransactionCADebit(debit);
        dto.setTransactionCACredit(credit);
        return dto;
    }

    // ========== CRÉER UN DTO LCL ==========
    public static TransactionLCLDto creerTransactionLCL(String date, String libelle, Double montant) {
        TransactionLCLDto dto = TransactionLCLDto.builder()
                .transactionLCLDate(parseDate(date))
                .transactionLCLLibelle(libelle)
                .transactionLCLMontant(montant)
                .build();
        return dto;
    }

    public static Long Compte;

    public static Long bank;


    // Méthode utilitaire pour parser les dates
    private static Date parseDate(String dateStr) {
        try {
            return sdf.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("Date invalide: " + dateStr);
        }
    }
}
