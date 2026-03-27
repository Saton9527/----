package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ProblemsetCreateRequest(
        @NotBlank String url,
        String title
) {
}
