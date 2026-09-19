package com.prizm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prizm.ai.AnalysisPipeline;
import com.prizm.api.dto.ArtifactResponse;
import com.prizm.api.error.ApiException;
import com.prizm.domain.Artifact;
import com.prizm.domain.Member;
import com.prizm.domain.Space;
import com.prizm.realtime.SpaceEventPublisher;
import com.prizm.repository.ArtifactRepository;
import com.prizm.repository.MemberRepository;
import com.prizm.repository.SpaceRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ArtifactService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final ArtifactRepository artifactRepository;
    private final AnalysisPipeline analysisPipeline;
    private final ObjectMapper objectMapper;
    private final SpaceEventPublisher spaceEventPublisher;

    public ArtifactService(
            SpaceRepository spaceRepository,
            MemberRepository memberRepository,
            ArtifactRepository artifactRepository,
            AnalysisPipeline analysisPipeline,
            ObjectMapper objectMapper,
            SpaceEventPublisher spaceEventPublisher
    ) {
        this.spaceRepository = spaceRepository;
        this.memberRepository = memberRepository;
        this.artifactRepository = artifactRepository;
        this.analysisPipeline = analysisPipeline;
        this.objectMapper = objectMapper;
        this.spaceEventPublisher = spaceEventPublisher;
    }

    @Transactional
    public ArtifactResponse create(Long spaceId, Long memberId, String title, String content) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "스페이스를 찾을 수 없습니다."));
        Member member = memberRepository.findByIdAndSpaceId(memberId, spaceId)
                .orElseThrow(() -> ApiException.forbidden("MEMBER_NOT_IN_SPACE", "해당 스페이스의 참여자가 아닙니다."));
        Artifact artifact = new Artifact(
                space,
                member,
                requireText(title, "TITLE_REQUIRED", "제목을 입력하세요."),
                requireText(content, "CONTENT_REQUIRED", "본문을 입력하세요.")
        );
        artifact.setTags("[]");
        Artifact saved = artifactRepository.save(artifact);
        ArtifactResponse response = toResponse(saved);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                analysisPipeline.enqueue(saved.getId());
                spaceEventPublisher.artifactAdded(spaceId, Map.of(
                        "id", saved.getId(),
                        "title", saved.getTitle(),
                        "status", saved.getStatus().name(),
                        "nickname", member.getNickname(),
                        "school", member.getSchool(),
                        "major", member.getMajor()
                ));
            }
        });
        return response;
    }

    @Transactional(readOnly = true)
    public List<ArtifactResponse> list(Long spaceId) {
        if (!spaceRepository.existsById(spaceId)) {
            throw ApiException.notFound("SPACE_NOT_FOUND", "스페이스를 찾을 수 없습니다.");
        }
        return artifactRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtifactResponse get(Long id) {
        return toResponse(artifactRepository.findDetailById(id)
                .orElseThrow(() -> ApiException.notFound("ARTIFACT_NOT_FOUND", "결과물을 찾을 수 없습니다.")));
    }

    public ArtifactResponse toResponse(Artifact artifact) {
        Long groupId = artifact.getGroup() == null ? null : artifact.getGroup().getId();
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getSpace().getId(),
                artifact.getMember().getId(),
                artifact.getMember().getNickname(),
                artifact.getMember().getSchool(),
                artifact.getMember().getMajor(),
                artifact.getTitle(),
                artifact.getContent(),
                artifact.getStatus(),
                parseTags(artifact),
                groupId,
                artifact.getCreatedAt()
        );
    }

    public List<String> parseTags(Artifact artifact) {
        if (artifact.getTags() == null || artifact.getTags().isBlank()) {
            return List.of();
        }
        try {
            List<String> tags = objectMapper.readValue(artifact.getTags(), STRING_LIST);
            return tags == null ? List.of() : tags;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String requireText(String value, String error, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(error, message);
        }
        return value.trim();
    }
}
