package com.agregateur.dimsoft.agregateur_production.services;

import com.agregateur.dimsoft.agregateur_production.beans.Bank;
import com.agregateur.dimsoft.agregateur_production.beans.Compte;
import com.agregateur.dimsoft.agregateur_production.models.AgregationResultDto;
import com.agregateur.dimsoft.agregateur_production.models.TransactionCADto;
import com.agregateur.dimsoft.agregateur_production.models.TransactionLCLDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BudgetAggregationService {
    public AgregationResultDto aggregateCATransaction(List<TransactionCADto> transactions, Compte compte, Bank bank);
    public AgregationResultDto aggregateLCLTransactions(List<TransactionLCLDto> transactions, Compte compte, Bank bank);
}