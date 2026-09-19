package com.prizm.api.dto;

import com.prizm.domain.ArtifactStatus;
import java.time.Instant;
import java.util.List;

public record ArtifactResponse(
        Long id,
        Long spaceId,
        Long memberId,
        String nickname,
        String school,
        String major,
        String title,
        String content,
        ArtifactStatus status,
        List<String> tags,
        Long groupId,
        Instant createdAt
) {
}
