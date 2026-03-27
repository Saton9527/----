package com.acmtrain.backend.dto;

public record AlertResponse(
        Long id,
        String userName,
        String ruleCode,
        String riskLevel,
        String hitTime,
        String status
) {
}
