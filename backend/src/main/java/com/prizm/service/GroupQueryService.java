package com.prizm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prizm.api.error.ApiException;
import com.prizm.domain.Artifact;
import com.prizm.domain.Group;
import com.prizm.repository.ArtifactRepository;
import com.prizm.repository.GroupRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupQueryService {

    private final GroupRepository groupRepository;
    private final ArtifactRepository artifactRepository;
    private final ObjectMapper objectMapper;

    public GroupQueryService(
            GroupRepository groupRepository,
            ArtifactRepository artifactRepository,
            ObjectMapper objectMapper
    ) {
        this.groupRepository = groupRepository;
        this.artifactRepository = artifactRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다."));
        List<Artifact> artifacts = artifactRepository.findByGroupId(id);
        List<Map<String, Object>> artifactViews = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            artifactViews.add(Map.of(
                    "id", artifact.getId(),
                    "title", artifact.getTitle(),
                    "memberNickname", artifact.getMember().getNickname(),
                    "school", artifact.getMember().getSchool(),
                    "major", artifact.getMember().getMajor()
            ));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", group.getId());
        body.put("spaceId", group.getSpace().getId());
        body.put("label", group.getLabel());
        body.put("commonPoints", group.getCommonPoints());
        body.put("differences", parseDifferences(group.getDifferences()));
        body.put("notes", group.getNotes());
        body.put("artifacts", artifactViews);
        return body;
    }

    Object parseDifferences(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {
                });
            }
            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }
}
