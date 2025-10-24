package com.agregateur.dimsoft.agregateur_production.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileCleanupScheduler {
    @Value("${file.upload.directory:src/main/resources/data/fichiers}")
    private String fileDirectory;

    // Exécuter toutes les heures
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOldFiles() {
        File directory = new File(fileDirectory);
        if (!directory.exists()) return;

        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < oneHourAgo) {
                    file.delete();
                }
            }
        }
    }
}
