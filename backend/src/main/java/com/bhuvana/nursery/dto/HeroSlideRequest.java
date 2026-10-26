package com.bhuvana.nursery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HeroSlideRequest(
        @NotBlank @Size(max = 2000)
        String imageUrl,

        @Size(max = 300)
        String altText,

        @Size(max = 200)
        String titleKn,

        @Size(max = 200)
        String titleEn,

        int displayOrder,

        boolean active
) {}
