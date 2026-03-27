package com.acmtrain.backend.dto;

public record PointLogResponse(
        Long id,
        String sourceType,
        String reason,
        Integer points,
        String createdAt
) {
}
