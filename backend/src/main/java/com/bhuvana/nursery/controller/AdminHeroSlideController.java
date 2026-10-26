package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.dto.HeroSlideRequest;
import com.bhuvana.nursery.dto.HeroSlideResponse;
import com.bhuvana.nursery.service.HeroSlideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/hero-slides")
public class AdminHeroSlideController {

    private final HeroSlideService heroSlideService;

    public AdminHeroSlideController(HeroSlideService heroSlideService) {
        this.heroSlideService = heroSlideService;
    }

    @GetMapping
    public List<HeroSlideResponse> getAllHeroSlides() {
        return heroSlideService.getAllSlides();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HeroSlideResponse create(@Valid @RequestBody HeroSlideRequest request) {
        return heroSlideService.create(request);
    }

    @PutMapping("/{id}")
    public HeroSlideResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody HeroSlideRequest request
    ) {
        return heroSlideService.update(id, request);
    }

    @PatchMapping("/{id}/toggle-active")
    public HeroSlideResponse toggleActive(@PathVariable UUID id) {
        return heroSlideService.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        heroSlideService.delete(id);
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@RequestBody List<UUID> orderedIds) {
        heroSlideService.reorder(orderedIds);
    }

    @PostMapping("/reset-defaults")
    public List<HeroSlideResponse> resetToDefaults() {
        return heroSlideService.resetToDefaults();
    }
}
