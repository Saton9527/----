package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CoachTaskCreateRequest(
        @NotNull Long teamId,
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String deadline,
        List<Long> assigneeUserIds
) {
}
