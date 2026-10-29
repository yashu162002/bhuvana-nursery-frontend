package com.bhuvana.nursery.config;

import com.bhuvana.nursery.storage.ImageStorage;
import com.bhuvana.nursery.storage.LocalImageStorage;
import com.bhuvana.nursery.storage.R2ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the image storage backend from {@code IMAGE_STORAGE}.
 *
 * <ul>
 *   <li>{@code local} (default) keeps development behaviour unchanged.</li>
 *   <li>{@code r2} requires the R2_* variables; startup fails immediately with
 *       a clear message if one is missing, rather than silently writing images
 *       to an ephemeral Render filesystem.</li>
 * </ul>
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    @ConditionalOnProperty(
            name = "app.storage.provider",
            havingValue = "local",
            matchIfMissing = true
    )
    public ImageStorage localImageStorage(
            @Value("${app.upload.directory:./uploads}") String directory
    ) {
        log.info("Image storage: local directory (not persistent on Render).");
        return new LocalImageStorage(Path.of(directory));
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
    public S3Client r2S3Client(
            @Value("${app.storage.r2.endpoint:}") String endpoint,
            @Value("${app.storage.r2.region:auto}") String region,
            @Value("${app.storage.r2.access-key:}") String accessKey,
            @Value("${app.storage.r2.secret-key:}") String secretKey
    ) {
        requireR2Configuration(
                "R2_ENDPOINT", endpoint,
                "R2_ACCESS_KEY_ID", accessKey,
                "R2_SECRET_ACCESS_KEY", secretKey
        );

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
    public ImageStorage r2ImageStorage(
            S3Client r2S3Client,
            @Value("${app.storage.r2.bucket:}") String bucket,
            @Value("${app.storage.r2.public-base-url:}") String publicBaseUrl
    ) {
        requireR2Configuration(
                "R2_BUCKET", bucket,
                "R2_PUBLIC_BASE_URL", publicBaseUrl
        );

        if (!publicBaseUrl.startsWith("https://")
                && !publicBaseUrl.startsWith("http://")) {
            throw new IllegalStateException(
                    "R2_PUBLIC_BASE_URL must be the public http(s) URL of the bucket "
                            + "or custom image domain.");
        }

        log.info("Image storage: Cloudflare R2 bucket '{}'.", bucket);
        return new R2ImageStorage(r2S3Client, bucket, publicBaseUrl);
    }

    private static void requireR2Configuration(String... namesAndValues) {
        List<String> missing = new ArrayList<>();

        for (int index = 0; index < namesAndValues.length; index += 2) {
            String value = namesAndValues[index + 1];
            if (value == null || value.isBlank()) {
                missing.add(namesAndValues[index]);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "IMAGE_STORAGE=r2 requires these environment variables: "
                            + String.join(", ", missing));
        }
    }
}
