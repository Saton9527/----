package com.acmtrain.backend.dto;

public record AlertResponse(
        Long id,
        Long userId,
        String userName,
        String ruleCode,
        String riskLevel,
        String hitTime,
        String status,
        String description,
        String suspiciousProblems,
        String suggestion,
        String mailSentAt,
        String studentFeedback,
        String feedbackAt
) {
}
