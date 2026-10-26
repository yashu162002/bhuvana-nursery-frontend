package com.bhuvana.nursery.dto;

import java.time.Instant;
import java.util.UUID;

public record HeroSlideResponse(
        UUID id,
        String imageUrl,
        String altText,
        String titleKn,
        String titleEn,
        int displayOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
