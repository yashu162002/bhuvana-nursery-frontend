package com.bhuvana.nursery.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Development storage: files live under {@code app.upload.directory} and are
 * served by the backend at {@code /uploads/**}.
 *
 * <p>This is intentionally not production-safe on PaaS platforms such as
 * Render, whose filesystem is replaced on every deploy. Use
 * {@code IMAGE_STORAGE=r2} there.</p>
 */
public class LocalImageStorage implements ImageStorage {

    private static final String URL_PREFIX = "/uploads/";

    private final Path uploadDirectory;

    public LocalImageStorage(Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory.toAbsolutePath().normalize();
    }

    @Override
    public StoredImage store(byte[] content, String contentType, String extension) {
        String filename = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(uploadDirectory);
            Path target = resolveInsideUploadDirectory(filename);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new ImageStorageException(
                    "Could not write the uploaded image to disk.", exception);
        }

        return new StoredImage(URL_PREFIX + filename, filename);
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }

        String filename = url.substring(URL_PREFIX.length());

        // Only generated names are ever stored by this class, so anything with
        // a separator, a query string or a traversal segment is not ours.
        if (filename.isBlank()
                || filename.contains("/")
                || filename.contains("\\")
                || filename.contains("..")
                || filename.contains("?")) {
            return;
        }

        Path target = resolveInsideUploadDirectory(filename);

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new ImageStorageException(
                    "Could not delete an unused local image.", exception);
        }
    }

    private Path resolveInsideUploadDirectory(String filename) {
        Path target = uploadDirectory.resolve(filename).normalize();

        if (!target.startsWith(uploadDirectory)) {
            throw new ImageStorageException(
                    "Refusing to access a path outside the upload directory.",
                    new IllegalArgumentException(filename));
        }

        return target;
    }
}
