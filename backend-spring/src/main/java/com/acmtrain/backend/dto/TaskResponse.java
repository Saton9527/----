package com.acmtrain.backend.dto;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String deadline,
        String status,
        Integer totalProblems,
        Integer completedProblems
) {
}
