package com.acmtrain.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String deadline,
        String status,
        @Min(1) Integer totalProblems
) {
}
