package com.acmtrain.backend.dto;

public record MyProfileSyncJobResponse(
        String jobId,
        String status,
        String message,
        String startedAt,
        String finishedAt,
        MyProfileResponse profile
) {
}
