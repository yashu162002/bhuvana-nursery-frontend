package com.bhuvana.nursery.dto;

import com.bhuvana.nursery.entity.AvailabilityStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PlantResponse(
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
    boolean featured,
    String imageUrl
) {}
