package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ContestCreateRequest(
        @NotBlank String url,
        String title
) {
}
