package com.bhuvana.nursery.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryAdminResponse(
        UUID id,
        String slug,
        String nameKn,
        String nameEn,
        String descriptionKn,
        String descriptionEn,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {}
