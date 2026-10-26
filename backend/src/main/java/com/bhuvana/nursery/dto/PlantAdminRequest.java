package com.bhuvana.nursery.dto;

import com.bhuvana.nursery.entity.AvailabilityStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PlantAdminRequest(
        @NotNull UUID categoryId,

        @NotBlank @Size(max = 200)
        String nameKn,

        @NotBlank @Size(max = 200)
        String nameEn,

        @Size(max = 250)
        String scientificName,

        @Size(max = 5000)
        String descriptionKn,

        @Size(max = 5000)
        String descriptionEn,

        @Size(max = 220)
        String slug,

        @DecimalMin("0.00")
        BigDecimal price,

        boolean enquiryOnly,

        @NotNull
        AvailabilityStatus availability,

        boolean published,

        boolean featured,

        @Size(max = 2000)
        String imageUrl
) {}
