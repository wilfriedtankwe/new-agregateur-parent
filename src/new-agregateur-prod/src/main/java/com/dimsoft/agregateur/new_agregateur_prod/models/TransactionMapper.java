package com.dimsoft.agregateur.new_agregateur_prod.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class TransactionMapper {

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Convertit une ligne CA en TransactionCADto
     */
    public static TransactionCADto convertirCAEnDto(Map<String, String> ligneCA) {
        TransactionCADto dto = new TransactionCADto();

        try {
            // Date
            String dateStr = ligneCA.get("Date");
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    dto.setTransactionCADate(dateFormatter.parse(dateStr));
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Date invalide : " + dateStr);
                }
            }

            // Libellé
            String libelle = ligneCA.get("Libellé");
            if (libelle == null) {
                libelle = ligneCA.get("Libelle");  // Alternative sans accent
            }
            dto.setTransactionCALibelle(libelle != null ? libelle.trim() : "");

            // Débit
            String debitStr = ligneCA.get("Débit euros");
            if (debitStr == null) {
                debitStr = ligneCA.get("Debit euros");  // Alternative sans accent
            }
            if (debitStr != null && !debitStr.isEmpty()) {
                dto.setTransactionCADebit(convertirMontant(debitStr));
            }

            // Crédit
            String creditStr = ligneCA.get("Crédit euros");
            if (creditStr == null) {
                creditStr = ligneCA.get("Credit euros");  // Alternative sans accent
            }
            if (creditStr != null && !creditStr.isEmpty()) {
                dto.setTransactionCACredit(convertirMontant(creditStr));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la conversion CA en DTO : " + e.getMessage(), e);
        }

        return dto;
    }


    /**
     * Convertit une ligne LCL en TransactionLCLDto
     */
    public static TransactionLCLDto convertirLCLEnDto(Map<String, String> ligneLCL) {
        TransactionLCLDto dto = new TransactionLCLDto();

        try {
            // Date
            String dateStr = ligneLCL.get("Date");
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    dto.setTransactionLCLDate(dateFormatter.parse(dateStr));
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Date invalide : " + dateStr);
                }
            }

            // Montant (peut être négatif ou positif)
            String montantStr = ligneLCL.get("Montant");
            if (montantStr != null && !montantStr.isEmpty()) {
                dto.setTransactionLCLMontant(convertirMontant(montantStr));
            }

            // Libellé (Mode de paiement + Détails)
            String modePaiement = ligneLCL.get("Mode de paiement");
            String libelle = ligneLCL.get("Libellé");

            String libelleComplet = "";
            if (modePaiement != null && !modePaiement.isEmpty()) {
                libelleComplet = modePaiement.trim();
            }
            if (libelle != null && !libelle.isEmpty()) {
                libelleComplet += (libelleComplet.isEmpty() ? "" : " - ") + libelle.trim();
            }

            dto.setTransactionLCLLibelle(libelleComplet);

            // Notes (Divers)
            String divers = ligneLCL.get("Divers");
            dto.setTransactionNotes(divers != null ? divers.trim() : "");

            // Numéro de chèque (si présent)
            String chequeStr = ligneLCL.get("Chèque");
            if (chequeStr != null && !chequeStr.isEmpty()) {
                try {
                    dto.setTransactionChequeNumber(Long.parseLong(chequeStr));
                } catch (NumberFormatException e) {
                    // Ignorer si ce n'est pas un nombre
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la conversion LCL en DTO : " + e.getMessage(), e);
        }

        return dto;
    }


    /**
     * Convertit un montant en format français (1 234,56) en Double
     */
    private static Double convertirMontant(String montantStr) {
        if (montantStr == null || montantStr.isEmpty()) {
            return 0.0;
        }

        montantStr = montantStr.replace(" ", "").trim();
        montantStr = montantStr.replace(",", ".");

        try {
            return Double.parseDouble(montantStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant invalide : " + montantStr);
        }
    }
}

