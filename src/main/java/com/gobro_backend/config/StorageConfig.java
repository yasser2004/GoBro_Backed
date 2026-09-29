package com.gobro_backend.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Storage configuration.
 *
 * The platform supports several interchangeable object storage backends for
 * course videos and assets (see StorageService / S3StorageService / MinioStorageService):
 *  - AWS S3 (production)
 *  - Cloudflare R2 (S3-compatible, cost-effective for egress-heavy video delivery)
 *  - MinIO (local/dev, self-hosted, S3-compatible)
 *
 * Active provider is chosen via app.storage.provider = s3 | r2 | minio
 */
@Configuration
public class StorageConfig {

    @Value("${app.storage.provider:minio}")
    private String provider;

    @Value("${app.storage.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.region:us-east-1}")
    private String region;

    @Value("${app.storage.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.storage.secret-key:minioadmin}")
    private String secretKey;

    @Value("${app.storage.bucket:gobro-videos}")
    private String bucket;

    @Value("${app.storage.path-style-access:true}")
    private boolean pathStyleAccess;

    /**
     * Generic S3-compatible client, used directly for AWS S3 and Cloudflare R2
     * (both speak the native S3 API). For R2, app.storage.endpoint should be
     * the account's R2 endpoint (https://<account_id>.r2.cloudflarestorage.com).
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());

        if (!"s3".equalsIgnoreCase(provider)) {
            // R2 / custom S3-compatible endpoints require an explicit override.
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    /**
     * Presigner used to generate short-lived upload/download URLs for course
     * videos and certificates without proxying bytes through the API server.
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (!"s3".equalsIgnoreCase(provider)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    /**
     * Native MinIO client, used by MinioStorageService for local/dev setups
     * where MinIO-specific features (bucket policies, notifications) are needed.
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String getBucket() {
        return bucket;
    }
}