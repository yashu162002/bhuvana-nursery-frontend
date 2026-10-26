package com.bhuvana.nursery.service;

import com.bhuvana.nursery.dto.PlantAdminRequest;
import com.bhuvana.nursery.dto.PlantAdminResponse;
import com.bhuvana.nursery.entity.Plant;
import com.bhuvana.nursery.entity.PlantCategory;
import com.bhuvana.nursery.repository.PlantCategoryRepository;
import com.bhuvana.nursery.repository.PlantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlantAdminService {

    private final PlantRepository plantRepository;
    private final PlantCategoryRepository categoryRepository;

    public PlantAdminService(
            PlantRepository plantRepository,
            PlantCategoryRepository categoryRepository
    ) {
        this.plantRepository = plantRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PlantAdminResponse> getAllPlants() {
        return plantRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlantAdminResponse getPlant(UUID id) {
        return toResponse(findPlant(id));
    }

    @Transactional
    public PlantAdminResponse create(PlantAdminRequest request) {
        validateRequest(request);

        Plant plant = new Plant();
        applyRequest(plant, request);

        plant.setArchived(false);

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantAdminResponse update(UUID id, PlantAdminRequest request) {
        validateRequest(request);

        Plant plant = findPlant(id);
        applyRequest(plant, request);

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantAdminResponse setPublished(UUID id, boolean published) {
        Plant plant = findPlant(id);
        plant.setPublished(published);

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantAdminResponse archive(UUID id) {
        Plant plant = findPlant(id);
        plant.setArchived(true);
        plant.setPublished(false);

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantAdminResponse restore(UUID id) {
        Plant plant = findPlant(id);
        plant.setArchived(false);

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public void deletePermanent(UUID id) {
        Plant plant = findPlant(id);
        plantRepository.delete(plant);
    }

    private void applyRequest(Plant plant, PlantAdminRequest request) {
        String slug = makeSlug(request.slug(), request.nameEn());

        boolean duplicate = plant.getId() == null
                ? plantRepository.existsBySlug(slug)
                : plantRepository.existsBySlugAndIdNot(slug, plant.getId());

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A plant with this slug already exists."
            );
        }

        PlantCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selected category does not exist."
                ));

        plant.setCategory(category);
        plant.setSlug(slug);
        plant.setNameKn(request.nameKn().trim());
        plant.setNameEn(request.nameEn().trim());
        plant.setScientificName(trimToNull(request.scientificName()));
        plant.setDescriptionKn(trimToNull(request.descriptionKn()));
        plant.setDescriptionEn(trimToNull(request.descriptionEn()));
        plant.setPrice(request.price());
        plant.setEnquiryOnly(request.enquiryOnly());
        plant.setAvailability(request.availability());
        plant.setPublished(request.published());
        plant.setFeatured(request.featured());
        plant.setImageUrl(validateImageUrl(request.imageUrl()));

        if (!request.enquiryOnly() && request.price() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A price is required when enquiry-only is disabled."
            );
        }
    }

    private void validateRequest(PlantAdminRequest request) {
        if (request.price() != null && request.price().signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price cannot be negative."
            );
        }
    }

    private String makeSlug(String requestedSlug, String nameEn) {
        String source = requestedSlug == null || requestedSlug.isBlank()
                ? nameEn
                : requestedSlug;

        String slug = source.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid English name or URL slug."
            );
        }

        if (slug.length() > 220) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slug must be 220 characters or fewer."
            );
        }

        return slug;
    }

    private String validateImageUrl(String rawUrl) {
        String value = trimToNull(rawUrl);

        if (value == null) {
            return null;
        }

        if (value.startsWith("/uploads/") && value.length() <= 2000) {
            return value;
        }

        try {
            URI uri = URI.create(value);

            if (!("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || value.length() > 2000) {
                throw new IllegalArgumentException();
            }

            return value;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image URL must be a valid HTTP or HTTPS URL or an uploaded image path."
            );
        }
    }

    private Plant findPlant(UUID id) {
        return plantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plant not found."
                ));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PlantAdminResponse toResponse(Plant plant) {
        PlantCategory category = plant.getCategory();

        return new PlantAdminResponse(
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
                plant.isPublished(),
                plant.isFeatured(),
                plant.isArchived(),
                plant.getImageUrl(),
                plant.getCreatedAt(),
                plant.getUpdatedAt()
        );
    }
}
