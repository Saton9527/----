package com.acmtrain.backend.dto;

public record ProblemDetailResponse(
        String problemCode,
        String title,
        Integer rating,
        String tag,
        String bucketLabel
) {
}
