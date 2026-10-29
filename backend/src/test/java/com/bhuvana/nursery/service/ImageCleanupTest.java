package com.bhuvana.nursery.service;

import com.bhuvana.nursery.repository.HeroSlideRepository;
import com.bhuvana.nursery.repository.PlantRepository;
import com.bhuvana.nursery.storage.ImageStorage;
import com.bhuvana.nursery.storage.ImageStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageCleanupTest {

    private final ImageStorage imageStorage = mock(ImageStorage.class);
    private final PlantRepository plantRepository = mock(PlantRepository.class);
    private final HeroSlideRepository heroSlideRepository =
            mock(HeroSlideRepository.class);
    private final ImageCleanup cleanup = new ImageCleanup(
            imageStorage, plantRepository, heroSlideRepository);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesImmediatelyWhenThereIsNoTransaction() {
        when(plantRepository.existsByImageUrl("/uploads/a.jpg")).thenReturn(false);
        when(heroSlideRepository.existsByImageUrl("/uploads/a.jpg")).thenReturn(false);

        cleanup.deleteWhenUnusedAfterCommit("/uploads/a.jpg");

        verify(imageStorage).delete("/uploads/a.jpg");
    }

    @Test
    void waitsForCommitBeforeDeleting() {
        TransactionSynchronizationManager.initSynchronization();

        cleanup.deleteWhenUnusedAfterCommit("/uploads/a.jpg");
        verify(imageStorage, never()).delete(anyString());

        when(plantRepository.existsByImageUrl("/uploads/a.jpg")).thenReturn(false);
        when(heroSlideRepository.existsByImageUrl("/uploads/a.jpg")).thenReturn(false);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(
                        TransactionSynchronization.STATUS_COMMITTED));

        verify(imageStorage).delete("/uploads/a.jpg");
    }

    @Test
    void neverDeletesAfterARollback() {
        TransactionSynchronizationManager.initSynchronization();

        cleanup.deleteWhenUnusedAfterCommit("/uploads/a.jpg");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void keepsAnImageStillUsedByAPlantOrSlide() {
        when(plantRepository.existsByImageUrl("/uploads/shared.jpg")).thenReturn(true);

        cleanup.deleteWhenUnusedAfterCommit("/uploads/shared.jpg");

        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void keepsAnImageStillUsedByAHeroSlide() {
        when(plantRepository.existsByImageUrl("/uploads/shared.jpg")).thenReturn(false);
        when(heroSlideRepository.existsByImageUrl("/uploads/shared.jpg")).thenReturn(true);

        cleanup.deleteWhenUnusedAfterCommit("/uploads/shared.jpg");

        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void storageFailuresNeverFailTheAdminRequest() {
        when(plantRepository.existsByImageUrl("/uploads/a.jpg")).thenReturn(false);
        when(heroSlideRepository.existsByImageUrl("/uploads/a.jpg")).thenReturn(false);
        doThrow(new ImageStorageException("storage down", new RuntimeException()))
                .when(imageStorage).delete("/uploads/a.jpg");

        assertThatCode(() -> cleanup.deleteWhenUnusedAfterCommit("/uploads/a.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresBlankUrls() {
        cleanup.deleteWhenUnusedAfterCommit(null);
        cleanup.deleteWhenUnusedAfterCommit("  ");

        verifyNoInteractions(imageStorage, plantRepository, heroSlideRepository);
    }
}
