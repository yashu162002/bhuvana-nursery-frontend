package com.bhuvana.nursery.storage;

/**
 * Storage backend failure. The message is safe to log and is never returned
 * verbatim to the browser.
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
