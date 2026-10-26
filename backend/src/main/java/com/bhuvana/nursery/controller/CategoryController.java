package com.bhuvana.nursery.controller;

import com.bhuvana.nursery.dto.CategoryResponse;
import com.bhuvana.nursery.service.PlantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final PlantService plantService;

    public CategoryController(PlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return plantService.getCategories();
    }
}
