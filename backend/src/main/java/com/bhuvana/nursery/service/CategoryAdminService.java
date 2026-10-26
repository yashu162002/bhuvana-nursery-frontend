package com.bhuvana.nursery.service;

import com.bhuvana.nursery.dto.CategoryAdminRequest;
import com.bhuvana.nursery.dto.CategoryAdminResponse;
import com.bhuvana.nursery.entity.PlantCategory;
import com.bhuvana.nursery.repository.PlantCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CategoryAdminService {

    private final PlantCategoryRepository repository;

    public CategoryAdminService(PlantCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryAdminResponse> getAll() {
        return repository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryAdminResponse create(CategoryAdminRequest request) {
        PlantCategory category = new PlantCategory();
        apply(category, request);
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryAdminResponse update(
            UUID id,
            CategoryAdminRequest request
    ) {
        PlantCategory category = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"
                ));

        apply(category, request);
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryAdminResponse setPublished(UUID id, boolean published) {
        PlantCategory category = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"
                ));

        if (!published) {
            // Hide plants from the public catalogue when their category is hidden.
            // Their individual publication settings are retained.
            category.setPublished(false);
        } else {
            category.setPublished(true);
        }

        return toResponse(repository.save(category));
    }

    private void apply(
            PlantCategory category,
            CategoryAdminRequest request
    ) {
        String source = request.slug() == null || request.slug().isBlank()
                ? request.nameEn()
                : request.slug();

        String slug = source.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank() || slug.length() > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enter a valid category slug"
            );
        }

        boolean duplicate = category.getId() == null
                ? repository.existsBySlug(slug)
                : repository.existsBySlugAndIdNot(slug, category.getId());

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A category with this slug already exists"
            );
        }

        category.setSlug(slug);
        category.setNameKn(request.nameKn().trim());
        category.setNameEn(request.nameEn().trim());
        category.setDescriptionKn(clean(request.descriptionKn()));
        category.setDescriptionEn(clean(request.descriptionEn()));
        category.setPublished(request.published());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CategoryAdminResponse toResponse(PlantCategory category) {
        return new CategoryAdminResponse(
                category.getId(),
                category.getSlug(),
                category.getNameKn(),
                category.getNameEn(),
                category.getDescriptionKn(),
                category.getDescriptionEn(),
                category.isPublished(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
