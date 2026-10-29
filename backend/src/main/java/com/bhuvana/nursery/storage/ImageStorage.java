package com.bhuvana.nursery.storage;

/**
 * Persists admin-uploaded images and returns the public URL stored in the
 * database.
 *
 * <p>Two implementations exist: a local filesystem store for development and a
 * Cloudflare R2 store for production, where the backend filesystem is
 * ephemeral. The upload API and the database contract (the {@code url} string)
 * stay identical for both.</p>
 */
public interface ImageStorage {

    /**
     * Stores already-validated image bytes.
     *
     * @param content     image bytes, validated by the caller
     * @param contentType canonical MIME type, for example {@code image/png}
     * @param extension   file extension including the dot, for example {@code .png}
     * @return the public URL and the generated filename
     * @throws ImageStorageException when the image could not be persisted
     */
    StoredImage store(byte[] content, String contentType, String extension);

    /**
     * Best-effort deletion of a previously stored image. Callers own the
     * decision of whether the URL is still referenced, and must tolerate a
     * failure here: a missing object is never fatal.
     *
     * @throws ImageStorageException when deletion failed, for logging only
     */
    void delete(String url);
}
