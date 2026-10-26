package com.bhuvana.nursery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryAdminRequest(
        @NotBlank @Size(max = 150)
        String nameKn,

        @NotBlank @Size(max = 150)
        String nameEn,

        @Size(max = 180)
        String slug,

        @Size(max = 5000)
        String descriptionKn,

        @Size(max = 5000)
        String descriptionEn,

        boolean published
) {}
