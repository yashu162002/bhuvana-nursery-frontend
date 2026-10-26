package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.dto.PageResponse;
import com.bhuvana.nursery.dto.PlantResponse;
import com.bhuvana.nursery.service.PlantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plants")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping
    public PageResponse<PlantResponse> getPlants(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return plantService.getPlants(category, search, page, size);
    }

    @GetMapping("/{slug}")
    public PlantResponse getPlantBySlug(@PathVariable String slug) {
        return plantService.getPlantBySlug(slug);
    }
}
