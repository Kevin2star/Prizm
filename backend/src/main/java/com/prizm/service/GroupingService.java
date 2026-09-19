package com.prizm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prizm.ai.CosineSimilarity;
import com.prizm.ai.GeminiClient;
import com.prizm.config.PrizmProperties;
import com.prizm.domain.Artifact;
import com.prizm.domain.ArtifactStatus;
import com.prizm.domain.Group;
import com.prizm.repository.ArtifactRepository;
import com.prizm.repository.GroupRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupingService {

    private static final Logger log = LoggerFactory.getLogger(GroupingService.class);
    private static final TypeReference<double[]> DOUBLE_ARRAY = new TypeReference<>() {
    };

    private final ArtifactRepository artifactRepository;
    private final GroupRepository groupRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final PrizmProperties properties;

    public GroupingService(
            ArtifactRepository artifactRepository,
            GroupRepository groupRepository,
            GeminiClient geminiClient,
            ObjectMapper objectMapper,
            PrizmProperties properties
    ) {
        this.artifactRepository = artifactRepository;
        this.groupRepository = groupRepository;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public Long assignGroup(Long artifactId) {
        Artifact current = artifactRepository.findDetailById(artifactId).orElse(null);
        if (current == null || current.getEmbedding() == null || current.getStatus() != ArtifactStatus.READY) {
            return null;
        }
        double[] currentVec = parseEmbedding(current.getEmbedding());
        List<Artifact> others = artifactRepository.findBySpaceId(current.getSpace().getId()).stream()
                .filter(item -> !item.getId().equals(current.getId()))
                .filter(item -> item.getStatus() == ArtifactStatus.READY)
                .filter(item -> item.getEmbedding() != null)
                .toList();

        Artifact best = null;
        double bestScore = -1;
        for (Artifact other : others) {
            double score = similarity(current, currentVec, other);
            if (score > bestScore) {
                bestScore = score;
                best = other;
            }
        }

        if (best == null || bestScore < properties.getSimilarityThreshold()) {
            return null;
        }

        Group group = best.getGroup();
        if (group == null) {
            group = groupRepository.save(new Group(current.getSpace(), "새 그룹"));
            best.setGroup(group);
            artifactRepository.save(best);
        }
        current.setGroup(group);
        artifactRepository.save(current);

        List<Artifact> members = artifactRepository.findByGroupId(group.getId());
        if (members.size() >= 2 && geminiClient.isConfigured()) {
            refreshSummary(group, members);
        } else if (members.size() >= 2) {
            fillFallbackSummary(group, members);
        }
        return group.getId();
    }

    private void refreshSummary(Group group, List<Artifact> members) {
        StringBuilder input = new StringBuilder();
        for (Artifact artifact : members) {
            input.append("- id=").append(artifact.getId())
                    .append(" nickname=").append(artifact.getMember().getNickname())
                    .append(" school=").append(artifact.getMember().getSchool())
                    .append(" major=").append(artifact.getMember().getMajor())
                    .append(" title=").append(artifact.getTitle())
                    .append(" tags=").append(artifact.getTags())
                    .append("\ncontent=").append(artifact.getContent())
                    .append("\n");
        }
        String prompt = """
                아래 결과물들을 하나로 합치지 말고 비교 정리본 JSON만 반환하라. 마크다운 금지.
                원문에 없는 사실을 만들지 마라.
                차이점은 "누가 / 어떤 관점으로 / 무엇을 다르게" 형식.
                notes는 빈틈/특이점/확장만. 없는 칭찬을 지어내지 마라.
                스키마:
                {"label":"...","commonPoints":"...","differences":[{"artifactId":1,"summary":"..."}],"notes":"..."}
                결과물:
                %s
                """.formatted(input);
        try {
            String raw = geminiClient.stripJson(geminiClient.generateContent(
                    "gemini-2.5-flash",
                    prompt
            ));
            JsonNode node = objectMapper.readTree(raw);
            group.setLabel(node.path("label").asText(group.getLabel()));
            group.setCommonPoints(node.path("commonPoints").asText(""));
            group.setDifferences(objectMapper.writeValueAsString(node.path("differences")));
            group.setNotes(node.path("notes").asText(""));
            groupRepository.save(group);
        } catch (Exception ex) {
            log.warn("Failed to refresh group summary for {}", group.getId());
            fillFallbackSummary(group, members);
        }
    }

    private void fillFallbackSummary(Group group, List<Artifact> members) {
        group.setLabel(members.get(0).getTitle());
        group.setCommonPoints("같은 문제의식을 공유하는 결과물들이 모였습니다.");
        List<Map<String, Object>> diffs = new ArrayList<>();
        for (Artifact artifact : members) {
            diffs.add(Map.of(
                    "artifactId", artifact.getId(),
                    "summary", artifact.getMember().getNickname()
                            + "(" + artifact.getMember().getMajor() + ") / "
                            + artifact.getTitle()
            ));
        }
        try {
            group.setDifferences(objectMapper.writeValueAsString(diffs));
        } catch (JsonProcessingException ignored) {
            group.setDifferences("[]");
        }
        group.setNotes("자동 폴백 정리본입니다. Gemini 키가 있으면 다시 분석됩니다.");
        groupRepository.save(group);
    }

    private double similarity(Artifact current, double[] currentVec, Artifact other) {
        double cosine = CosineSimilarity.cosine(currentVec, parseEmbedding(other.getEmbedding()));
        double boosted = cosine;
        if (shareTag(current, other)) {
            boosted = Math.max(boosted, 0.78);
        }
        if (mentionsWait(current) && mentionsWait(other)) {
            boosted = Math.max(boosted, 0.8);
        }
        if (mentionsSeat(current) && mentionsSeat(other)) {
            boosted = Math.max(boosted, 0.8);
        }
        return boosted;
    }

    private boolean shareTag(Artifact a, Artifact b) {
        List<String> left = parseTagList(a.getTags());
        List<String> right = parseTagList(b.getTags());
        return left.stream().anyMatch(right::contains);
    }

    private List<String> parseTagList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean mentionsWait(Artifact artifact) {
        String text = (artifact.getTitle() + artifact.getContent() + artifact.getTags()).toLowerCase();
        return text.contains("대기") || text.contains("줄") || text.contains("피크");
    }

    private boolean mentionsSeat(Artifact artifact) {
        String text = artifact.getTitle() + artifact.getContent() + artifact.getTags();
        return text.contains("좌석") || text.contains("공간") || text.contains("자리");
    }

    private double[] parseEmbedding(String json) {
        try {
            return objectMapper.readValue(json, DOUBLE_ARRAY);
        } catch (Exception ex) {
            return new double[0];
        }
    }
}
