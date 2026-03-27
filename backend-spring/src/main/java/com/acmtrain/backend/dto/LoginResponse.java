package com.acmtrain.backend.dto;

public record LoginResponse(
        String token,
        UserProfile user
) {
}
