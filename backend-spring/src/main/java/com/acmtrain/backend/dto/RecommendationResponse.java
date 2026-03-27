package com.acmtrain.backend.dto;

public record RecommendationResponse(
        Long id,
        String level,
        String problemCode,
        String title,
        Integer suggestedRating,
        Integer hiddenRating,
        String reason
) {
}