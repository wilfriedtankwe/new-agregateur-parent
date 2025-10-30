package com.dimsoft.agregateur.new_agregateur_prod.models;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour le résultat d'une agrégation de transactions
 */
public class AggregationResultDto {

    private int totalTransactions;
    private int savedTransactions;
    private int duplicateTransactions;
    private boolean success;
    private String message;
    private List<String> messages = new ArrayList<>();
    private List<Long> savedIds = new ArrayList<>();
    private List<Long> duplicateIds = new ArrayList<>();

    // ========== CONSTRUCTEURS ==========

    public AggregationResultDto() {
    }

    // ========== MÉTHODES STATIQUES (Factory) ==========

    /**
     * Succès : toutes les transactions ont été enregistrées
     */
    public static AggregationResultDto success(String message, int savedCount) {
        AggregationResultDto result = new AggregationResultDto();
        result.setSuccess(true);
        result.setMessage(message);
        result.setSavedTransactions(savedCount);
        result.setDuplicateTransactions(0);
        result.setTotalTransactions(savedCount);
        return result;
    }

    /**
     * Succès avec des doublons ignorés
     */
    public static AggregationResultDto successWithSkipped(String message, int savedCount, int skippedCount) {
        AggregationResultDto result = new AggregationResultDto();
        result.setSuccess(true);
        result.setMessage(message);
        result.setSavedTransactions(savedCount);
        result.setDuplicateTransactions(skippedCount);
        result.setTotalTransactions(savedCount + skippedCount);
        return result;
    }

    /**
     * Opération annulée
     */
    public static AggregationResultDto cancelled(String message) {
        AggregationResultDto result = new AggregationResultDto();
        result.setSuccess(false);
        result.setMessage(message);
        result.setSavedTransactions(0);
        result.setDuplicateTransactions(0);
        result.setTotalTransactions(0);
        return result;
    }

    // ========== GETTERS / SETTERS ==========

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public int getSavedTransactions() {
        return savedTransactions;
    }

    public void setSavedTransactions(int savedTransactions) {
        this.savedTransactions = savedTransactions;
    }

    public int getDuplicateTransactions() {
        return duplicateTransactions;
    }

    public void setDuplicateTransactions(int duplicateTransactions) {
        this.duplicateTransactions = duplicateTransactions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public List<Long> getSavedIds() {
        return savedIds;
    }

    public void setSavedIds(List<Long> savedIds) {
        this.savedIds = savedIds;
    }

    public List<Long> getDuplicateIds() {
        return duplicateIds;
    }

    public void setDuplicateIds(List<Long> duplicateIds) {
        this.duplicateIds = duplicateIds;
    }

    // ========== MÉTHODES UTILITAIRES ==========

    public void addMessage(String msg) {
        this.messages.add(msg);
    }

    public void addSavedId(Long id) {
        this.savedIds.add(id);
        this.savedTransactions++;
    }

    public void addDuplicateId(Long id) {
        this.duplicateIds.add(id);
        this.duplicateTransactions++;
    }

    @Override
    public String toString() {
        return "AgregationResultDto{" +
                "total=" + totalTransactions +
                ", saved=" + savedTransactions +
                ", duplicates=" + duplicateTransactions +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}