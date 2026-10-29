package com.bhuvana.nursery.config;

import com.bhuvana.nursery.storage.ImageStorage;
import com.bhuvana.nursery.storage.LocalImageStorage;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageConfigTest {

    private final StorageConfig config = new StorageConfig();

    @Test
    void localIsTheDefaultBackend() {
        ImageStorage storage = config.localImageStorage("./uploads");

        assertThat(storage).isInstanceOf(LocalImageStorage.class);
    }

    @Test
    void r2StartupFailsWithANamedMissingVariable() {
        assertThatThrownBy(() -> config.r2S3Client("", "auto", "key", "secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_ENDPOINT");
    }

    @Test
    void r2StorageRequiresBucketAndPublicUrl() {
        assertThatThrownBy(() -> config.r2ImageStorage(null, "", "https://cdn.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_BUCKET");

        assertThatThrownBy(() -> config.r2ImageStorage(null, "bucket", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_PUBLIC_BASE_URL");
    }

    @Test
    void r2ClientBuildsWithoutNetworkAccess() {
        try (S3Client client = config.r2S3Client(
                "https://example.r2.cloudflarestorage.com",
                "auto",
                "key",
                "secret"
        )) {
            assertThat(client).isNotNull();
            assertThat(client.serviceName()).isEqualTo("s3");
        }
    }

    @Test
    void r2PublicUrlMustBeHttp() {
        assertThatThrownBy(() -> config.r2ImageStorage(null, "bucket", "cdn.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_PUBLIC_BASE_URL");
    }
}
