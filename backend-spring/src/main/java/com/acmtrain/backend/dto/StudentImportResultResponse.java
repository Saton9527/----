package com.acmtrain.backend.dto;

import java.util.List;

public record StudentImportResultResponse(
        Integer importedCount,
        Integer createdCount,
        Integer updatedCount,
        Integer skippedCount,
        List<String> errors
) {
}
