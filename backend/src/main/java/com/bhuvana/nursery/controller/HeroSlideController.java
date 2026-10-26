package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.dto.HeroSlideResponse;
import com.bhuvana.nursery.service.HeroSlideService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hero-slides")
public class HeroSlideController {

    private final HeroSlideService heroSlideService;

    public HeroSlideController(HeroSlideService heroSlideService) {
        this.heroSlideService = heroSlideService;
    }

    @GetMapping
    public List<HeroSlideResponse> getActiveHeroSlides() {
        return heroSlideService.getActiveSlides();
    }
}
