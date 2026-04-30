package com.acmtrain.backend.dto;

public record StudentSyncJobResponse(
        String jobId,
        Long studentId,
        String status,
        String message,
        String startedAt,
        String finishedAt,
        StudentResponse student
) {
}
