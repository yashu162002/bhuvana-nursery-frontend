package com.bhuvana.nursery.service;

import com.bhuvana.nursery.repository.HeroSlideRepository;
import com.bhuvana.nursery.repository.PlantRepository;
import com.bhuvana.nursery.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Deletes an uploaded image only after the database transaction that stopped
 * referencing it has committed, and only when no other plant or hero slide
 * still uses the same URL.
 *
 * <p>This ordering guarantees that a failed update never destroys the current
 * image, and a shared URL is never removed from under another record. Cleanup
 * is best-effort: storage errors are logged but never fail the admin request.</p>
 */
@Service
public class ImageCleanup {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanup.class);

    private final ImageStorage imageStorage;
    private final PlantRepository plantRepository;
    private final HeroSlideRepository heroSlideRepository;

    public ImageCleanup(
            ImageStorage imageStorage,
            PlantRepository plantRepository,
            HeroSlideRepository heroSlideRepository
    ) {
        this.imageStorage = imageStorage;
        this.plantRepository = plantRepository;
        this.heroSlideRepository = heroSlideRepository;
    }

    public void deleteWhenUnusedAfterCommit(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteIfUnused(imageUrl);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            deleteIfUnused(imageUrl);
                        }
                    }
                }
        );
    }

    private void deleteIfUnused(String imageUrl) {
        try {
            if (plantRepository.existsByImageUrl(imageUrl)
                    || heroSlideRepository.existsByImageUrl(imageUrl)) {
                return;
            }

            imageStorage.delete(imageUrl);
        } catch (RuntimeException exception) {
            // A stale object is harmless; never turn cleanup into a failed
            // admin request. Log only the failure type, never SDK details.
            log.warn("Could not clean up an unused image ({}).",
                    exception.getClass().getSimpleName());
        }
    }
}
