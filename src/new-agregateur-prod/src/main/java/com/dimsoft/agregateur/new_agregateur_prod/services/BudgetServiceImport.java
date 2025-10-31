package com.dimsoft.agregateur.new_agregateur_prod.services;

import com.dimsoft.agregateur.new_agregateur_prod.models.AggregationResultDto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionCADto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionLCLDto;

import java.util.List;

public interface BudgetServiceImport {
    public AggregationResultDto importCATransactions(List<TransactionCADto> transactionCADtos, Long compte, Long bank);
    public AggregationResultDto importLCLTransactions(List<TransactionLCLDto> transactionLCLDtos , Long compte, Long bank);
}
