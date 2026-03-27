package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoachTaskCreateRequest(
        @NotNull Long teamId,
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String deadline
) {
}
