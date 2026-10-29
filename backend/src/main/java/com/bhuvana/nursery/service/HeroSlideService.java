package com.bhuvana.nursery.service;

import com.bhuvana.nursery.dto.HeroSlideRequest;
import com.bhuvana.nursery.dto.HeroSlideResponse;
import com.bhuvana.nursery.entity.HeroSlide;
import com.bhuvana.nursery.repository.HeroSlideRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class HeroSlideService {

    private record DefaultSlide(String url, String alt, String kn, String en, int order) {}

    private static final List<DefaultSlide> DEFAULT_SLIDES = List.of(
            new DefaultSlide("https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=2400&q=80", "The green canopy", "ಹಸಿರು ಛತ್ರ", "The green canopy", 1),
            new DefaultSlide("https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=2400&q=80", "New beginnings", "ಹೊಸ ಚಿಗುರು", "New beginnings", 2),
            new DefaultSlide("https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=2400&q=80", "Woods of light", "ಬೆಳಕಿನ ಕಾನನ", "Woods of light", 3),
            new DefaultSlide("https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=2400&q=80", "Grown with care", "ಕಾಳಜಿಯಿಂದ ಬೆಳೆದದ್ದು", "Grown with care", 4),
            new DefaultSlide("https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=2400&q=80", "The path to your garden", "ನಿಮ್ಮ ತೋಟದ ಹಾದಿ", "The path to your garden", 5),
            new DefaultSlide("https://images.unsplash.com/photo-1463936575829-25148e1db1b8?auto=format&fit=crop&w=2400&q=80", "Botanical beauty", "ಸಸ್ಯ ಸೊಬಗು", "Botanical beauty", 6)
    );

    private final HeroSlideRepository repository;
    private final ImageCleanup imageCleanup;

    public HeroSlideService(
            HeroSlideRepository repository,
            ImageCleanup imageCleanup
    ) {
        this.repository = repository;
        this.imageCleanup = imageCleanup;
    }

    @Transactional(readOnly = true)
    public List<HeroSlideResponse> getActiveSlides() {
        return repository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HeroSlideResponse> getAllSlides() {
        return repository.findAllByOrderByDisplayOrderAscCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HeroSlideResponse create(HeroSlideRequest request) {
        HeroSlide slide = new HeroSlide();
        slide.setImageUrl(request.imageUrl().trim());
        slide.setAltText(request.altText() != null ? request.altText().trim() : null);
        slide.setTitleKn(request.titleKn() != null ? request.titleKn().trim() : null);
        slide.setTitleEn(request.titleEn() != null ? request.titleEn().trim() : null);

        int order = request.displayOrder();
        if (order <= 0) {
            order = repository.findAll().size() + 1;
        }
        slide.setDisplayOrder(order);
        slide.setActive(request.active());

        return toResponse(repository.save(slide));
    }

    @Transactional
    public HeroSlideResponse update(UUID id, HeroSlideRequest request) {
        HeroSlide slide = findSlide(id);
        String previousImageUrl = slide.getImageUrl();

        slide.setImageUrl(request.imageUrl().trim());
        slide.setAltText(request.altText() != null ? request.altText().trim() : null);
        slide.setTitleKn(request.titleKn() != null ? request.titleKn().trim() : null);
        slide.setTitleEn(request.titleEn() != null ? request.titleEn().trim() : null);
        slide.setDisplayOrder(request.displayOrder());
        slide.setActive(request.active());

        HeroSlide saved = repository.save(slide);

        if (!Objects.equals(previousImageUrl, saved.getImageUrl())) {
            imageCleanup.deleteWhenUnusedAfterCommit(previousImageUrl);
        }

        return toResponse(saved);
    }

    @Transactional
    public HeroSlideResponse toggleActive(UUID id) {
        HeroSlide slide = findSlide(id);
        slide.setActive(!slide.isActive());
        return toResponse(repository.save(slide));
    }

    @Transactional
    public void delete(UUID id) {
        HeroSlide slide = findSlide(id);
        String previousImageUrl = slide.getImageUrl();

        repository.delete(slide);
        imageCleanup.deleteWhenUnusedAfterCommit(previousImageUrl);
    }

    @Transactional
    public void reorder(List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            final int order = i + 1;
            repository.findById(id).ifPresent(slide -> {
                slide.setDisplayOrder(order);
                repository.save(slide);
            });
        }
    }

    @Transactional
    public List<HeroSlideResponse> resetToDefaults() {
        List<String> previousImageUrls = repository.findAll()
                .stream()
                .map(HeroSlide::getImageUrl)
                .toList();

        repository.deleteAll();
        for (DefaultSlide def : DEFAULT_SLIDES) {
            HeroSlide slide = new HeroSlide();
            slide.setImageUrl(def.url());
            slide.setAltText(def.alt());
            slide.setTitleKn(def.kn());
            slide.setTitleEn(def.en());
            slide.setDisplayOrder(def.order());
            slide.setActive(true);
            repository.save(slide);
        }

        previousImageUrls.forEach(imageCleanup::deleteWhenUnusedAfterCommit);

        return getAllSlides();
    }

    private HeroSlide findSlide(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hero slide not found"
                ));
    }

    private HeroSlideResponse toResponse(HeroSlide slide) {
        return new HeroSlideResponse(
                slide.getId(),
                slide.getImageUrl(),
                slide.getAltText(),
                slide.getTitleKn(),
                slide.getTitleEn(),
                slide.getDisplayOrder(),
                slide.isActive(),
                slide.getCreatedAt(),
                slide.getUpdatedAt()
        );
    }
}
