package com.bhuvana.nursery.dto;

import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String slug,
    String nameKn,
    String nameEn,
    String descriptionKn,
    String descriptionEn
) {}
