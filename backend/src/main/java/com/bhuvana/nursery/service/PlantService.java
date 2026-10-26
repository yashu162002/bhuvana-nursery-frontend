package com.bhuvana.nursery.service;

import com.bhuvana.nursery.dto.CategoryResponse;
import com.bhuvana.nursery.dto.PageResponse;
import com.bhuvana.nursery.dto.PlantResponse;
import com.bhuvana.nursery.entity.Plant;
import com.bhuvana.nursery.entity.PlantCategory;
import com.bhuvana.nursery.repository.PlantCategoryRepository;
import com.bhuvana.nursery.repository.PlantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PlantService {

    private final PlantRepository plantRepository;
    private final PlantCategoryRepository categoryRepository;

    public PlantService(
        PlantRepository plantRepository,
        PlantCategoryRepository categoryRepository
    ) {
        this.plantRepository = plantRepository;
        this.categoryRepository = categoryRepository;
    }

    public PageResponse<PlantResponse> getPlants(
        String category,
        String search,
        int page,
        int size
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page must be zero or greater"
            );
        }

        if (size < 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Size must be at least 1"
            );
        }

        int safeSize = Math.min(size, 50);
        String categorySlug = normalize(category);
        String searchTerm = normalize(search);

        if (categorySlug != null && !categoryRepository.existsBySlug(categorySlug)) {
            // An unknown category should return an empty list, not all plants.
            return new PageResponse<>(
                java.util.List.of(),
                page,
                safeSize,
                0,
                0,
                true,
                true
            );
        }

        Pageable pageable = PageRequest.of(
            page,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<PlantResponse> result = plantRepository
            .findPublicPlants(categorySlug, searchTerm, pageable)
            .map(this::toPlantResponse);

        return PageResponse.from(result);
    }

    public PlantResponse getPlantBySlug(String slug) {
        Plant plant = plantRepository
            .findBySlugAndPublishedTrueAndArchivedFalse(slug)
            .filter(p -> p.getCategory().isPublished())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Plant not found"
            ));

        return toPlantResponse(plant);
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository
            .findByPublishedTrueOrderByNameEnAsc()
            .stream()
            .map(this::toCategoryResponse)
            .toList();
    }

    private PlantResponse toPlantResponse(Plant plant) {
        PlantCategory category = plant.getCategory();

        return new PlantResponse(
            plant.getId(),
            plant.getSlug(),
            plant.getNameKn(),
            plant.getNameEn(),
            plant.getScientificName(),
            plant.getDescriptionKn(),
            plant.getDescriptionEn(),
            category.getId(),
            category.getSlug(),
            category.getNameKn(),
            category.getNameEn(),
            plant.getPrice(),
            plant.isEnquiryOnly(),
            plant.getAvailability(),
            plant.isFeatured(),
            plant.getImageUrl()
        );
    }

    private CategoryResponse toCategoryResponse(PlantCategory category) {
        return new CategoryResponse(
            category.getId(),
            category.getSlug(),
            category.getNameKn(),
            category.getNameEn(),
            category.getDescriptionKn(),
            category.getDescriptionEn()
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
