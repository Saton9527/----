package com.acmtrain.backend.dto;

import java.math.BigDecimal;

public record RankingResponse(
        Integer rankNo,
        String userName,
        Integer cfRating,
        Integer atcRating,
        BigDecimal totalPoints,
        Integer solvedCount,
        Integer streakDays
) {
}
