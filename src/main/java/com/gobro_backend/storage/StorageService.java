package com.gobro_backend.storage;


import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Unified file storage for course thumbnails, downloadable resources,
 * certificates and (mainly, via presigned URLs) course videos — cahier des
 * charges §14.3 (Stockage : Cloudflare R2 / MinIO / AWS S3) and §15
 * (Architecture -> stockage objet, CDN).
 *
 * Delegates to whichever backend {@code app.storage.provider} selects in
 * StorageConfig: AWS S3 / Cloudflare R2 both speak the native S3 API
 * (via {@link S3Client}/{@link S3Presigner}), MinIO uses its own client
 * for local/dev environments.
 *
 * Large video uploads should go through {@link #generatePresignedUploadUrl}
 * so the browser/Angular app uploads directly to the object store instead
 * of proxying bytes through this API server.
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioClient minioClient;

    @Value("${app.storage.provider:minio}")
    private String provider;

    @Value("${app.storage.bucket:gobro-videos}")
    private String bucket;

    @Value("${app.storage.public-base-url:http://localhost:9000/gobro-videos}")
    private String publicBaseUrl;

    @Value("${app.storage.presigned-url-expiration-minutes:15}")
    private long presignedUrlExpirationMinutes;

    private boolean isMinio() {
        return "minio".equalsIgnoreCase(provider);
    }

    /**
     * Direct upload through the API server — suitable for small files
     * (avatars, thumbnails, PDF resources). Videos should use
     * {@link #generatePresignedUploadUrl} instead.
     */
    public UploadResult uploadFile(MultipartFile file, String folder) {
        String key = buildKey(folder, file.getOriginalFilename());

        try {
            if (isMinio()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            } else {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize())
                );
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de l'upload du fichier : " + e.getMessage(), e);
        }

        return new UploadResult(key, buildPublicUrl(key));
    }

    /**
     * Generates a short-lived URL the client can PUT the file bytes to
     * directly (video uploads), bypassing the API server entirely.
     */
    public PresignedUrl generatePresignedUploadUrl(String fileName, String contentType, String folder) {
        String key = buildKey(folder, fileName);
        Duration expiry = Duration.ofMinutes(presignedUrlExpirationMinutes);

        String url;
        if (isMinio()) {
            url = presignMinioUrl(key, Method.PUT, expiry);
        } else {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build())
                    .build();
            url = s3Presigner.presignPutObject(presignRequest).url().toString();
        }

        return new PresignedUrl(key, url, expiry.toSeconds());
    }

    /**
     * Generates a short-lived URL to read a private object (e.g. an
     * unpublished course video, a generated certificate PDF).
     */
    public PresignedUrl generatePresignedDownloadUrl(String key) {
        Duration expiry = Duration.ofMinutes(presignedUrlExpirationMinutes);

        String url;
        if (isMinio()) {
            url = presignMinioUrl(key, Method.GET, expiry);
        } else {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .build();
            url = s3Presigner.presignGetObject(presignRequest).url().toString();
        }

        return new PresignedUrl(key, url, expiry.toSeconds());
    }

    public void deleteFile(String key) {
        try {
            if (isMinio()) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build());
            } else {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la suppression du fichier : " + e.getMessage(), e);
        }
    }

    // ---------- Helpers ----------

    private String presignMinioUrl(String key, Method method, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(method)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) expiry.toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la génération de l'URL présignée : " + e.getMessage(), e);
        }
    }

    private String buildKey(String folder, String originalFileName) {
        String safeFolder = (folder == null || folder.isBlank()) ? "misc" : folder.replaceAll("^/+|/+$", "");
        String extension = extractExtension(originalFileName);
        return safeFolder + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String buildPublicUrl(String key) {
        return publicBaseUrl.replaceAll("/+$", "") + "/" + key;
    }

    // ---------- Result types ----------

    public record UploadResult(String key, String url) {
    }

    public record PresignedUrl(String key, String url, long expiresInSeconds) {
    }
}