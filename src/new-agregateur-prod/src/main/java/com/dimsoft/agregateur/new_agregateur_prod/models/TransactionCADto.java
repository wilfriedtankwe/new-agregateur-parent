package com.dimsoft.agregateur.new_agregateur_prod.models;



import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
public class TransactionCADto {
    private Date transactionCADate;

    private String transactionCALibelle;

    private Double transactionCADebit;

    private Double transactionCACredit;

    public TransactionCADto() {
        // constructeur vide obligatoire
    }

    public TransactionCADto(Date date, String libelle, Double debit, Double credit) {
        this.transactionCADate = date;
        this.transactionCALibelle = libelle;
        this.transactionCADebit = debit;
        this.transactionCACredit = credit;
    }
}

