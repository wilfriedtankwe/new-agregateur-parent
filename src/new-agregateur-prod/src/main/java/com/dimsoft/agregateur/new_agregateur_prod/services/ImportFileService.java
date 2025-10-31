package com.dimsoft.agregateur.new_agregateur_prod.services;


import com.dimsoft.agregateur.new_agregateur_prod.services.impl.ImportFileServiceImpl;

import java.util.Map;

public interface ImportFileService {
    Map<String, ImportFileServiceImpl.AccountResult> extractNumbersFromFiles() ;


}
