package com.acmtrain.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateStudentRequest(
        @NotBlank String username,
        String password,
        @NotBlank String realName,
        @NotBlank String grade,
        @NotBlank String major,
        @NotBlank String cfHandle,
        String atcHandle,
        @Min(0) Integer cfRating,
        @Min(0) Integer atcRating,
        @Min(0) Integer solvedCount,
        @Min(0) Integer totalPoints
) {
}
