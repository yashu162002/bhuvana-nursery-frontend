package com.bhuvana.nursery.config;

import com.bhuvana.nursery.repository.HeroSlideRepository;
import com.bhuvana.nursery.repository.PlantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Warns when the database still contains development image paths while R2 is
 * active. Render's filesystem is ephemeral, so those files are not present in
 * production and the public site would show broken images even though new
 * uploads work correctly.
 *
 * <p>This is an operator signal only; it never changes data or fails startup.</p>
 */
@Component
public class LegacyImageMigrationWarning implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(LegacyImageMigrationWarning.class);

    private static final String LEGACY_PREFIX = "/uploads/";

    private final PlantRepository plantRepository;
    private final HeroSlideRepository heroSlideRepository;
    private final String storageProvider;

    public LegacyImageMigrationWarning(
            PlantRepository plantRepository,
            HeroSlideRepository heroSlideRepository,
            @Value("${app.storage.provider:local}") String storageProvider
    ) {
        this.plantRepository = plantRepository;
        this.heroSlideRepository = heroSlideRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"r2".equalsIgnoreCase(storageProvider)) {
            return;
        }

        long plants = plantRepository.countByImageUrlStartingWith(LEGACY_PREFIX);
        long slides = heroSlideRepository.countByImageUrlStartingWith(LEGACY_PREFIX);

        if (plants == 0 && slides == 0) {
            return;
        }

        log.warn(
                "{} plant(s) and {} hero slide(s) still reference local /uploads/ "
                        + "images. Copy those files to R2 under the uploads/ prefix, "
                        + "then rewrite the database URLs to R2_PUBLIC_BASE_URL. "
                        + "Otherwise the public site will show broken images.",
                plants,
                slides
        );
    }
}
