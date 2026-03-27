package com.acmtrain.backend.dto;

public record TeamInviteResponse(
        Long id,
        Long teamId,
        String teamName,
        String inviterName,
        String status,
        String createdAt
) {
}
