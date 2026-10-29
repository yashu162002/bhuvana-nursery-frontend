package com.bhuvana.nursery.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class R2ImageStorageTest {

    private static final String BUCKET = "bhuvana-images";
    private static final String PUBLIC_BASE = "https://images.bhuvanursery.com";

    private S3Client s3Client;
    private R2ImageStorage storage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        storage = new R2ImageStorage(s3Client, BUCKET, PUBLIC_BASE + "/");
    }

    @Test
    void storeUploadsUnderAUuidKeyAndReturnsThePublicUrl() {
        StoredImage stored = storage.store(new byte[]{1, 2}, "image/png", ".png");

        ArgumentCaptor<PutObjectRequest> captor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).startsWith("uploads/").endsWith(".png");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.cacheControl()).contains("max-age");

        assertThat(stored.url()).isEqualTo(PUBLIC_BASE + "/" + request.key());
        assertThat(stored.filename()).isEqualTo(request.key().substring("uploads/".length()));
    }

    @Test
    void deleteRemovesTheObjectForAnOwnedUrl() {
        StoredImage stored = storage.store(new byte[]{1}, "image/jpeg", ".jpg");

        storage.delete(stored.url());

        ArgumentCaptor<DeleteObjectRequest> captor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(stored.url()).endsWith(captor.getValue().key());
    }

    @Test
    void deleteIgnoresUrlsFromOtherHosts() {
        storage.delete("https://images.example.com/uploads/plant.png");
        storage.delete("https://images.bhuvanursery.com.evil.example/uploads/x.png");
        storage.delete("/uploads/plant.png");
        storage.delete(null);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void storeFailuresAreWrappedWithoutLeakingSdkDetails() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("network down"));

        assertThatThrownBy(() -> storage.store(new byte[]{1}, "image/png", ".png"))
                .isInstanceOf(ImageStorageException.class)
                .hasMessageContaining("object storage");
    }
}
