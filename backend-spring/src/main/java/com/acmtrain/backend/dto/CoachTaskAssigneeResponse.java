package com.acmtrain.backend.dto;

public record CoachTaskAssigneeResponse(
        Long assignmentId,
        Long userId,
        String username,
        String realName,
        String status,
        String completedAt
) {
}
