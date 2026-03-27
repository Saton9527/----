package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAssignmentStatusRequest(
        @NotBlank String status
) {
}
