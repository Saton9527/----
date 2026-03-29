package com.acmtrain.backend.dto;

import java.util.List;

public record DashboardAnalyticsResponse(
        Integer totalSolved,
        Integer hiddenRating,
        List<ProblemBucketResponse> buckets,
        List<ProblemTagResponse> tags,
        List<ProblemDetailResponse> recentSolved
) {
}
