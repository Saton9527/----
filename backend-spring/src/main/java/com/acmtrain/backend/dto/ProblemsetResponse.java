package com.acmtrain.backend.dto;

public record ProblemsetResponse(
        Long id,
        String platform,
        String title,
        String url,
        boolean solved,
        String solvedAt
) {
}