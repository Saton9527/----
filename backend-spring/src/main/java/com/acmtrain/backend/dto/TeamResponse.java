package com.acmtrain.backend.dto;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        Long coachId,
        String coachName,
        List<TeamMemberResponse> members
) {
}
