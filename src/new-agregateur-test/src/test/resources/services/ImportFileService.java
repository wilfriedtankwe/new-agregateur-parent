package com.agregateur.dimsoft.agregateur_production.services;

import com.agregateur.dimsoft.agregateur_production.services.impl.ImportFileServiceImpl;

import java.util.Map;

public interface ImportFileService {
    Map<String, ImportFileServiceImpl.AccountResult> extractNumbersFromFiles() ;


}
