package com.bhuvana.nursery.storage;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

/**
 * Cloudflare R2 storage. R2 is S3-compatible, so the AWS SDK v2 S3 client is
 * used with a custom endpoint and path-style access.
 *
 * <p>Objects are written under {@code uploads/} with a random UUID filename;
 * the bucket (or a custom domain in front of it) serves them publicly. The
 * access key and secret never leave the backend.</p>
 */
public class R2ImageStorage implements ImageStorage {

    private static final String KEY_PREFIX = "uploads/";
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2ImageStorage(S3Client s3Client, String bucket, String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public StoredImage store(byte[] content, String contentType, String extension) {
        String filename = UUID.randomUUID() + extension;
        String key = KEY_PREFIX + filename;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .cacheControl(CACHE_CONTROL)
                            .build(),
                    RequestBody.fromBytes(content)
            );
        } catch (SdkException exception) {
            throw new ImageStorageException(
                    "Could not upload the image to object storage.", exception);
        }

        return new StoredImage(publicBaseUrl + "/" + key, filename);
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(publicBaseUrl + "/")) {
            return;
        }

        String key = url.substring(publicBaseUrl.length() + 1);

        if (key.isBlank() || key.contains("..")) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (SdkException exception) {
            throw new ImageStorageException(
                    "Could not delete an unused object from storage.", exception);
        }
    }
}
