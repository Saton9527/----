package com.acmtrain.backend.dto;

public record UserProfile(
        Long id,
        String username,
        String realName,
        String role
) {
}
