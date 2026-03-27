package com.acmtrain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePlatformBindingRequest(
        @NotBlank(message = "cfHandle 不能为空")
        @Size(max = 64, message = "cfHandle 长度不能超过64")
        String cfHandle,
        @Size(max = 64, message = "atcHandle 长度不能超过64")
        String atcHandle
) {
}
