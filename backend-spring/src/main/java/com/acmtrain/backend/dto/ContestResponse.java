package com.acmtrain.backend.dto;

public record ContestResponse(
        Long id,
        String platform,
        String title,
        String url,
        String startTime,
        String reminderTime,
        Integer reminderMinutes,
        String status
) {
}
