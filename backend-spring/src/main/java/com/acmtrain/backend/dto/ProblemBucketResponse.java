package com.acmtrain.backend.dto;

public record ProblemBucketResponse(
        String rangeLabel,
        Integer solvedCount,
        Integer percentage
) {
}
