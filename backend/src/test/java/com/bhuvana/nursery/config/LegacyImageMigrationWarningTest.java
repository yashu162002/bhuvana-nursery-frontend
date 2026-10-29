package com.bhuvana.nursery.config;

import com.bhuvana.nursery.repository.HeroSlideRepository;
import com.bhuvana.nursery.repository.PlantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class LegacyImageMigrationWarningTest {

    private final PlantRepository plantRepository = mock(PlantRepository.class);
    private final HeroSlideRepository heroSlideRepository =
            mock(HeroSlideRepository.class);

    @Test
    void warnsWhenR2IsActiveAndLegacyPathsRemain(CapturedOutput output) {
        when(plantRepository.countByImageUrlStartingWith("/uploads/")).thenReturn(14L);
        when(heroSlideRepository.countByImageUrlStartingWith("/uploads/")).thenReturn(1L);

        warning("r2").run(null);

        assertThat(output.getAll())
                .contains("14 plant(s) and 1 hero slide(s)")
                .contains("R2_PUBLIC_BASE_URL");
    }

    @Test
    void staysQuietWhenNoLegacyPathsRemain(CapturedOutput output) {
        when(plantRepository.countByImageUrlStartingWith("/uploads/")).thenReturn(0L);
        when(heroSlideRepository.countByImageUrlStartingWith("/uploads/")).thenReturn(0L);

        warning("r2").run(null);

        assertThat(output.getAll()).doesNotContain("still reference local");
    }

    @Test
    void staysQuietForLocalStorage() {
        warning("local").run(null);

        verify(plantRepository, never()).countByImageUrlStartingWith("/uploads/");
        verify(heroSlideRepository, never()).countByImageUrlStartingWith("/uploads/");
    }

    private LegacyImageMigrationWarning warning(String provider) {
        return new LegacyImageMigrationWarning(
                plantRepository, heroSlideRepository, provider);
    }
}
