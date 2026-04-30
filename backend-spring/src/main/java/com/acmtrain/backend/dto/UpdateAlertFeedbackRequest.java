package com.acmtrain.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateAlertFeedbackRequest(
        @Size(max = 500, message = "反馈内容不能超过500个字符")
        String feedback
) {
}
