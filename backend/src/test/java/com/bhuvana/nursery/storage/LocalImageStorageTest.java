package com.bhuvana.nursery.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImageStorageTest {

    @TempDir
    Path uploadDirectory;

    @Test
    void storeWritesAUniquelyNamedFileAndReturnsItsUrl() throws Exception {
        LocalImageStorage storage = new LocalImageStorage(uploadDirectory);
        byte[] content = {1, 2, 3, 4};

        StoredImage stored = storage.store(content, "image/png", ".png");

        assertThat(stored.url()).startsWith("/uploads/").endsWith(".png");
        assertThat(stored.filename()).endsWith(".png");
        assertThat(uploadDirectory.resolve(stored.filename()))
                .exists()
                .hasBinaryContent(content);
    }

    @Test
    void storeNeverReusesAFilename() {
        LocalImageStorage storage = new LocalImageStorage(uploadDirectory);

        StoredImage first = storage.store(new byte[]{1}, "image/png", ".png");
        StoredImage second = storage.store(new byte[]{1}, "image/png", ".png");

        assertThat(first.filename()).isNotEqualTo(second.filename());
    }

    @Test
    void deleteRemovesTheStoredFile() {
        LocalImageStorage storage = new LocalImageStorage(uploadDirectory);
        StoredImage stored = storage.store(new byte[]{1}, "image/png", ".png");

        storage.delete(stored.url());

        assertThat(uploadDirectory.resolve(stored.filename())).doesNotExist();
    }

    @Test
    void deleteIgnoresExternalUrlsAndTraversalAttempts() throws Exception {
        LocalImageStorage storage = new LocalImageStorage(uploadDirectory);
        Path outside = uploadDirectory.getParent()
                .resolve("outside-" + UUID.randomUUID() + ".txt");
        Files.writeString(outside, "keep me");

        try {
            storage.delete("https://images.example.com/plant.png");
            storage.delete("/uploads/../" + outside.getFileName());
            storage.delete("/uploads/nested/file.png");
            storage.delete(null);

            assertThat(outside).exists();
        } finally {
            Files.deleteIfExists(outside);
        }
    }
}
