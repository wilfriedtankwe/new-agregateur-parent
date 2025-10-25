package com.dimsoft.agregateur.new_agregateur_prod.controller;


import lombok.var;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


@RestController
@RequestMapping("/api/files")
public class uploadFileController {
    @Value("${file.upload.directory:src/new-agregateur-prod/src/main/resources/data/fichiers}")
    private String FILE_DIRECTORY;

    /**
     * Upload et traitement d'un fichier unique
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Vérifier si un fichier est bien envoyé
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("❌ Aucun fichier n’a été envoyé.");
            }

            // Vérifier l’extension du fichier
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("❌ Seuls les fichiers CSV sont autorisés.");
            }

            // Dossier de destination
            String directoryPath = "src/agregateur-production/src/main/resources/data/fichiers";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Définir le chemin de destination
            Path destinationPath = Paths.get(directoryPath, fileName);

            // Copier le fichier (fermeture auto du flux)
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok("✅ Fichier CSV sauvegardé dans : " + destinationPath);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }

    /**
     * Méthode utilitaire : Sauvegarde dans un dossier du projet
     */
    private Path uploadFileToProjectDirectory(MultipartFile file) throws IOException {
        // 🔹 Récupère le chemin absolu du projet (là où le code tourne)
        String projectDir = System.getProperty("user.dir");

        // 🔹 Construit le chemin complet du dossier de stockage
        Path uploadDir = Paths.get(projectDir, FILE_DIRECTORY).toAbsolutePath();

        // 🔹 Crée le dossier s’il n’existe pas
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 🔹 Nom de fichier original
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "fichier_" + System.currentTimeMillis();
        }

        // 🔹 Crée le chemin complet du fichier
        Path filePath = uploadDir.resolve(originalFilename);

        // 🔹 Copie le contenu du fichier dans le dossier du projet
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }



    /**
     * Upload et traitement de plusieurs fichiers
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<Map<String, Object>> results = new ArrayList<>();

        String directoryPath = "src/new-agregateur-prod/src/main/resources/data/fichiers";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            if (files == null || files.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "❌ Aucun fichier n’a été envoyé.");
                return ResponseEntity.badRequest().body(error);
            }

            for (MultipartFile file : files) {
                Map<String, Object> fileResult = new HashMap<>();
                String fileName = file.getOriginalFilename();
                fileResult.put("fileName", fileName);

                if (file.isEmpty()) {
                    fileResult.put("status", "❌ Échec : fichier vide");
                    results.add(fileResult);
                    continue;
                }

                if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
                    fileResult.put("status", "❌ Échec : seuls les fichiers CSV sont autorisés");
                    results.add(fileResult);
                    continue;
                }

                Path destinationPath = Paths.get(directoryPath, fileName);

                // Vérifier si le fichier existe déjà
                if (Files.exists(destinationPath)) {
                    fileResult.put("status", "⚠️ Fichier déjà existant, ignoré");
                    fileResult.put("path", destinationPath.toString());
                    results.add(fileResult);
                    continue; // passer au fichier suivant
                }

                // Sauvegarder le fichier
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    fileResult.put("status", "✅ Fichier sauvegardé avec succès");
                    fileResult.put("path", destinationPath.toString());
                } catch (IOException e) {
                    fileResult.put("status", "❌ Erreur lors de la sauvegarde : " + e.getMessage());
                }

                results.add(fileResult);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Traitement des fichiers terminé");
            response.put("count", results.size());
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "❌ Erreur serveur : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


}
