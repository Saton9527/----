package com.acmtrain.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateStudentRequest(
        @NotBlank String username,
        String password,
        @NotBlank String realName,
        @NotBlank String grade,
        @NotBlank String major,
        String cfHandle,
        String atcHandle,
        @Min(0) Integer cfRating,
        @Min(0) Integer atcRating,
        @Min(0) Integer solvedCount,
        @DecimalMin("0.0") BigDecimal totalPoints
) {
}
