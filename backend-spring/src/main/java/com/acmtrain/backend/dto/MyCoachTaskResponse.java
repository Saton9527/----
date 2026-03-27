package com.acmtrain.backend.dto;

public record MyCoachTaskResponse(
        Long assignmentId,
        Long taskId,
        Long teamId,
        String title,
        String description,
        String deadline,
        String status,
        String coachName
) {
}
