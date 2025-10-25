package com.dimsoft.agregateur.new_agregateur_prod.controller;



import com.dimsoft.agregateur.new_agregateur_prod.services.impl.ImportFileServiceImpl;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@RequestMapping("/api/files")
public class ImportFileController {

    private final ImportFileServiceImpl importFileService;


    public ImportFileController(ImportFileServiceImpl importFileService) {
        this.importFileService = importFileService;

    }



    @GetMapping("/extract-account-number")
    public Map<String, ImportFileServiceImpl.AccountResult> extractNumbers1() {
        return importFileService.extractNumbersFromFiles();
    }




}
