package com.agregateur.dimsoft.agregateur_production.services;

import com.agregateur.dimsoft.agregateur_production.Enum.UserDecision;
import org.springframework.stereotype.Component;

@Component
public interface UserInteractionService {

    public UserDecision askUserDecision(String transactionInfo);
    public void displayMessage(String message);
}
