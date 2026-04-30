package com.acmtrain.backend.dto;

import java.util.List;

public record CoachTaskResponse(
        Long id,
        Long teamId,
        String teamName,
        String title,
        String description,
        String deadline,
        String createdAt,
        Integer assignedCount,
        Integer inProgressCount,
        Integer doneCount,
        List<CoachTaskAssigneeResponse> assignees
) {
}
