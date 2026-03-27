package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamInviteRequest(
        @NotBlank String username
) {
}
