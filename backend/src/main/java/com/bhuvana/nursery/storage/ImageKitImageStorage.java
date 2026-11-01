package com.bhuvana.nursery.storage;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * ImageKit cloud storage backend.
 *
 * <p>Images are uploaded via the official ImageKit Java SDK using the
 * private key for authentication. The public CDN URL returned by ImageKit
 * is stored in the database and stays valid indefinitely — it is never
 * affected by Render deploys or filesystem resets.</p>
 *
 * <p>Credentials are read from environment variables at startup via
 * {@link StorageConfig}; they are never logged or exposed to the frontend.</p>
 */
public class ImageKitImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageKitImageStorage.class);

    private final ImageKitClient client;
    private final String urlEndpoint;
    private final String folder;

    /**
     * @param publicKey   ImageKit public key (IMAGEKIT_PUBLIC_KEY)
     * @param privateKey  ImageKit private key (IMAGEKIT_PRIVATE_KEY) — never logged
     * @param urlEndpoint ImageKit URL endpoint (IMAGEKIT_URL_ENDPOINT)
     * @param folder      Destination folder inside ImageKit (IMAGEKIT_FOLDER)
     */
    public ImageKitImageStorage(String publicKey, String privateKey,
                                String urlEndpoint, String folder) {
        this.urlEndpoint = urlEndpoint.endsWith("/")
                ? urlEndpoint.substring(0, urlEndpoint.length() - 1)
                : urlEndpoint;
        this.folder = folder.startsWith("/") ? folder : "/" + folder;

        this.client = ImageKitOkHttpClient.builder()
                .publicKey(publicKey)
                .privateKey(privateKey)
                .urlEndpoint(urlEndpoint)
                .build();
    }

    @Override
    public StoredImage store(byte[] content, String contentType, String extension) {
        String filename = UUID.randomUUID() + extension;

        FileUploadParams params = FileUploadParams.builder()
                .file(content)
                .fileName(filename)
                .folder(folder)
                .useUniqueFileName(false)
                .build();

        try {
            FileUploadResponse response = client.files().upload(params);

            if (response == null || response.getUrl() == null || response.getUrl().isBlank()) {
                throw new ImageStorageException(
                        "ImageKit upload succeeded but returned no URL.",
                        new IllegalStateException("Null or blank URL in ImageKit response"));
            }

            log.info("Uploaded image to ImageKit: fileId={}, path={}",
                    response.getFileId(), response.getFilePath());

            return new StoredImage(response.getUrl(), response.getName());

        } catch (ImageStorageException storageException) {
            throw storageException;
        } catch (Exception exception) {
            throw new ImageStorageException(
                    "Could not upload the image to ImageKit.", exception);
        }
    }

    @Override
    public void delete(String url) {
        // Best-effort: deletion by URL alone requires knowing the ImageKit fileId,
        // which is not stored in the current schema. The old URL is logged so an
        // operator can clean up the ImageKit Media Library manually if desired.
        // A future migration can add a imagekit_file_id column to persist this.
        if (url != null && url.startsWith(urlEndpoint)) {
            log.info("Image replacement: old ImageKit URL {} is no longer referenced. "
                    + "Delete it from the ImageKit Media Library if storage space is a concern.",
                    url);
        }
    }
}
