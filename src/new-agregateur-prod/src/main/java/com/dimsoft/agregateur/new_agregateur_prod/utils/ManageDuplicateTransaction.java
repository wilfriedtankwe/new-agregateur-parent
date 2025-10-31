package com.dimsoft.agregateur.new_agregateur_prod.utils;


import lombok.AllArgsConstructor;



public enum ManageDuplicateTransaction {
    ANNULER("Annuler l'opération"),

    ENREGISTRER_QUAND_MEME("Enregistrer quand même"),

    SAUTER("Sauter et continuer");

    private final String description;

    ManageDuplicateTransaction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
