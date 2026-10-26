package com.bhuvana.nursery.dto;

import com.bhuvana.nursery.entity.AvailabilityStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlantAdminResponse(
        UUID id,
        String slug,
        String nameKn,
        String nameEn,
        String scientificName,
        String descriptionKn,
        String descriptionEn,
        UUID categoryId,
        String categorySlug,
        String categoryNameKn,
        String categoryNameEn,
        BigDecimal price,
        boolean enquiryOnly,
        AvailabilityStatus availability,
        boolean published,
        boolean featured,
        boolean archived,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt
) {}
