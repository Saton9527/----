package com.acmtrain.backend.dto;

public record RankingResponse(
        Integer rankNo,
        String userName,
        Integer cfRating,
        Integer atcRating,
        Integer totalPoints,
        Integer solvedCount,
        Integer streakDays
) {
}
