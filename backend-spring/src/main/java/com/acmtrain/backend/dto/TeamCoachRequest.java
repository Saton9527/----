package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamCoachRequest(
        @NotBlank String coachUsername
) {
}
