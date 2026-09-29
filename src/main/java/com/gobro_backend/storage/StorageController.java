package com.gobro_backend.storage;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * File storage endpoints — cahier des charges §14.3 (Stockage) and §7.4
 * (Vidéos : streaming). Small files (avatars, thumbnails, PDF resources)
 * go through direct upload; videos should use the presigned-URL flow so
 * the browser uploads straight to S3/R2/MinIO.
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Stockage", description = "Upload de fichiers et génération d'URLs présignées")
public class StorageController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Uploader un petit fichier (avatar, miniature, ressource PDF) via le serveur")
    public ResponseEntity<StorageService.UploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "misc") String folder
    ) {
        return ResponseEntity.ok(storageService.uploadFile(file, folder));
    }

    @GetMapping("/presigned-upload-url")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    @Operation(summary = "Obtenir une URL présignée pour uploader une vidéo directement vers le stockage")
    public ResponseEntity<StorageService.PresignedUrl> presignedUploadUrl(
            @RequestParam @NotBlank String fileName,
            @RequestParam(defaultValue = "video/mp4") String contentType,
            @RequestParam(defaultValue = "videos") String folder
    ) {
        return ResponseEntity.ok(storageService.generatePresignedUploadUrl(fileName, contentType, folder));
    }

    @GetMapping("/presigned-download-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtenir une URL présignée pour lire un fichier privé (vidéo non publiée, certificat...)")
    public ResponseEntity<StorageService.PresignedUrl> presignedDownloadUrl(
            @RequestParam @NotBlank String key
    ) {
        return ResponseEntity.ok(storageService.generatePresignedDownloadUrl(key));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    @Operation(summary = "Supprimer un fichier du stockage")
    public ResponseEntity<Void> delete(@RequestParam @NotBlank String key) {
        storageService.deleteFile(key);
        return ResponseEntity.noContent().build();
    }
}