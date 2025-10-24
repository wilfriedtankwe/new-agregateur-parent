package com.agregateur.dimsoft.agregateur_production.services.impl;

import com.agregateur.dimsoft.agregateur_production.Enum.UserDecision;
import com.agregateur.dimsoft.agregateur_production.services.UserInteractionService;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class UserInteractionServiceImplement implements UserInteractionService {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public UserDecision askUserDecision(String transactionInfo) {
        System.out.println("Transaction redondante détectée :\n" + transactionInfo);
        System.out.println("Que voulez-vous faire ?");
        System.out.println("1 - Annuler l'agrégation");
        System.out.println("2 - Continuer avec redondance");
        System.out.println("3 - Continuer sans redondance");

        int choice = 0;
        while (choice < 1 || choice > 3) {
            System.out.print("Entrez votre choix (1-3) : ");
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
            } else {
                scanner.next(); // ignorer l'entrée invalide
            }
        }

        switch (choice) {
            case 1: return UserDecision.ANNULER_AGREGATION;
            case 2: return UserDecision.CONTINUER_AVEC_REDONDANCE;
            case 3: return UserDecision.CONTINUER_SANS_REDONDANCE;
            default: return UserDecision.CONTINUER_SANS_REDONDANCE; // par défaut
        }
    }

    @Override
    public void displayMessage(String message) {
        System.out.println("[INFO] " + message);
    }
}
