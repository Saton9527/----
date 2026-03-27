package com.acmtrain.backend.dto;

import jakarta.validation.constraints.Min;

public record UpdateTaskProgressRequest(
        @Min(0) Integer completedProblems
) {
}
