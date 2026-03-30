package com.acmtrain.backend.dto;

public record ProblemCatalogResponse(
        Long id,
        String problemCode,
        String title,
        Integer rating,
        String tag,
        String platform,
        String bucketLabel,
        boolean solved,
        boolean recommended
) {
}
