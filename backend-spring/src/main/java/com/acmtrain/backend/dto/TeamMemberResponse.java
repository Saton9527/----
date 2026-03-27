package com.acmtrain.backend.dto;

public record TeamMemberResponse(
        Long userId,
        String username,
        String realName,
        String role
) {
}
