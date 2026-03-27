package com.acmtrain.backend.dto;

public record CoachTaskResponse(
        Long id,
        Long teamId,
        String title,
        String description,
        String deadline,
        String createdAt
) {
}
