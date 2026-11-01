package com.bhuvana.nursery.config;

import com.bhuvana.nursery.storage.ImageKitImageStorage;
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
 *   <li>{@code imagekit} uploads to ImageKit cloud storage (recommended for production).
 *       Requires {@code IMAGEKIT_PUBLIC_KEY}, {@code IMAGEKIT_PRIVATE_KEY},
 *       and {@code IMAGEKIT_URL_ENDPOINT}.</li>
 *   <li>{@code r2} requires the R2_* variables; startup fails immediately with
 *       a clear message if one is missing, rather than silently writing images
 *       to an ephemeral Render filesystem.</li>
 * </ul>
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    // -------------------------------------------------------------------------
    // Local (development default)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // ImageKit (recommended for production)
    // -------------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "imagekit")
    public ImageStorage imageKitImageStorage(
            @Value("${app.storage.imagekit.public-key:}") String publicKey,
            @Value("${app.storage.imagekit.private-key:}") String privateKey,
            @Value("${app.storage.imagekit.url-endpoint:}") String urlEndpoint,
            @Value("${app.storage.imagekit.folder:/bhuvana-nursery/uploads}") String folder
    ) {
        requireNonBlank(
                "imagekit",
                "IMAGEKIT_PUBLIC_KEY", publicKey,
                "IMAGEKIT_PRIVATE_KEY", privateKey,
                "IMAGEKIT_URL_ENDPOINT", urlEndpoint
        );

        if (!urlEndpoint.startsWith("https://") && !urlEndpoint.startsWith("http://")) {
            throw new IllegalStateException(
                    "IMAGEKIT_URL_ENDPOINT must be a valid http(s) URL "
                            + "(e.g. https://ik.imagekit.io/your-imagekit-id).");
        }

        log.info("Image storage: ImageKit (endpoint: {}, folder: {}).", urlEndpoint, folder);
        return new ImageKitImageStorage(publicKey, privateKey, urlEndpoint, folder);
    }

    // -------------------------------------------------------------------------
    // Cloudflare R2 (S3-compatible, alternative for production)
    // -------------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
    public S3Client r2S3Client(
            @Value("${app.storage.r2.endpoint:}") String endpoint,
            @Value("${app.storage.r2.region:auto}") String region,
            @Value("${app.storage.r2.access-key:}") String accessKey,
            @Value("${app.storage.r2.secret-key:}") String secretKey
    ) {
        requireNonBlank(
                "r2",
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
        requireNonBlank(
                "r2",
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

    // -------------------------------------------------------------------------
    // Shared validation helper
    // -------------------------------------------------------------------------

    /**
     * Fails fast at startup with a clear message when required variables are missing,
     * rather than letting images silently fall through to local/ephemeral storage.
     */
    private static void requireNonBlank(String provider, String... namesAndValues) {
        List<String> missing = new ArrayList<>();

        for (int index = 0; index < namesAndValues.length; index += 2) {
            String value = namesAndValues[index + 1];
            if (value == null || value.isBlank()) {
                missing.add(namesAndValues[index]);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "IMAGE_STORAGE=" + provider
                            + " requires these environment variables to be set: "
                            + String.join(", ", missing));
        }
    }
}
