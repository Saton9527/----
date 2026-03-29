package com.acmtrain.backend.dto;

public record StudentResponse(
        Long id,
        Long userId,
        String username,
        String realName,
        String grade,
        String major,
        String cfHandle,
        String atcHandle,
        Integer cfRating,
        Integer atcRating,
        Integer solvedCount,
        Integer totalPoints
) {
}
