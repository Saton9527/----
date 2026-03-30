package com.acmtrain.backend.dto;

public record OjContestHistoryResponse(
        Long id,
        String platform,
        String contestName,
        String contestUrl,
        String contestTime,
        Integer rankNo,
        Integer performance,
        Integer newRating,
        Integer ratingChange
) {
}
