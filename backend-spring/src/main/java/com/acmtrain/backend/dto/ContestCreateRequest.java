package com.acmtrain.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContestCreateRequest(
        @NotBlank String url,
        String title,
        @NotBlank String startTime,
        @NotNull @Min(0) Integer reminderMinutes
) {
}
