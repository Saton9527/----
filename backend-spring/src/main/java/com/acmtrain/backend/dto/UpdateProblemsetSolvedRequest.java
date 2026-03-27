package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProblemsetSolvedRequest(
        @NotNull(message = "solved 不能为空")
        Boolean solved
) {
}