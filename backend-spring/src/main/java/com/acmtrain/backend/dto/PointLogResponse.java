package com.acmtrain.backend.dto;

import java.math.BigDecimal;

public record PointLogResponse(
        Long id,
        String sourceType,
        String reason,
        BigDecimal points,
        String createdAt
) {
}
