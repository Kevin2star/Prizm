package com.prizm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prizm.api.error.ApiException;
import com.prizm.domain.Artifact;
import com.prizm.domain.Group;
import com.prizm.domain.Space;
import com.prizm.repository.ArtifactRepository;
import com.prizm.repository.GroupRepository;
import com.prizm.repository.SpaceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraphService {

    private final SpaceRepository spaceRepository;
    private final GroupRepository groupRepository;
    private final ArtifactRepository artifactRepository;
    private final ObjectMapper objectMapper;

    public GraphService(
            SpaceRepository spaceRepository,
            GroupRepository groupRepository,
            ArtifactRepository artifactRepository,
            ObjectMapper objectMapper
    ) {
        this.spaceRepository = spaceRepository;
        this.groupRepository = groupRepository;
        this.artifactRepository = artifactRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGraph(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "스페이스를 찾을 수 없습니다."));
        List<Artifact> artifacts = artifactRepository.findGraphBySpaceId(spaceId);
        List<Group> groups = groupRepository.findBySpaceId(spaceId);
        Map<Long, List<Artifact>> byGroup = artifacts.stream()
                .filter(item -> item.getGroup() != null)
                .collect(Collectors.groupingBy(item -> item.getGroup().getId()));

        List<Map<String, Object>> children = new ArrayList<>();
        groups.sort(Comparator.comparing(Group::getId));
        for (Group group : groups) {
            List<Artifact> members = byGroup.getOrDefault(group.getId(), List.of());
            if (!members.isEmpty()) {
                children.add(groupNode(group, members));
            }
        }
        artifacts.stream()
                .filter(item -> item.getGroup() == null)
                .sorted(Comparator.comparing(Artifact::getCreatedAt))
                .forEach(item -> children.add(artifactNode(item)));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "SPACE");
        root.put("id", "space-" + space.getId());
        root.put("label", space.getName());
        root.put("children", children);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("spaceId", space.getId());
        response.put("name", space.getName());
        response.put("joinCode", space.getJoinCode());
        response.put("root", root);
        return response;
    }

    private Map<String, Object> groupNode(Group group, List<Artifact> members) {
        List<Long> sourceIds = members.stream().map(Artifact::getId).toList();
        Map<Long, Artifact> byId = members.stream().collect(Collectors.toMap(Artifact::getId, item -> item));
        boolean updated = group.getUpdatedAt() != null
                && group.getUpdatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)));

        List<Map<String, Object>> diffItems = new ArrayList<>();
        for (Map<String, Object> row : parseDiff(group.getDifferences())) {
            Object rawId = row.get("artifactId");
            Long artifactId = rawId == null ? null : Long.valueOf(String.valueOf(rawId));
            Artifact source = artifactId == null ? null : byId.get(artifactId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("artifactId", artifactId);
            item.put("summary", String.valueOf(row.getOrDefault("summary", "")));
            if (source != null) {
                item.put("nickname", source.getMember().getNickname());
                item.put("school", source.getMember().getSchool());
                item.put("major", source.getMember().getMajor());
            }
            diffItems.add(item);
        }

        Map<String, Object> common = leaf("COMMON", "group-" + group.getId() + "-common", "공통점", group.getCommonPoints(), sourceIds);
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("type", "DIFF");
        diff.put("id", "group-" + group.getId() + "-diff");
        diff.put("label", "차이점");
        diff.put("items", diffItems);
        diff.put("sourceArtifactIds", sourceIds);

        Map<String, Object> notes = leaf("NOTES", "group-" + group.getId() + "-notes", "비고", group.getNotes(), sourceIds);

        List<Map<String, Object>> sourceChildren = members.stream().map(this::artifactNode).toList();
        Map<String, Object> sources = new LinkedHashMap<>();
        sources.put("type", "SOURCES");
        sources.put("id", "group-" + group.getId() + "-sources");
        sources.put("label", "원본 목록");
        sources.put("children", sourceChildren);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "GROUP");
        node.put("id", "group-" + group.getId());
        node.put("groupId", group.getId());
        node.put("label", group.getLabel());
        node.put("updated", updated);
        node.put("children", List.of(common, diff, notes, sources));
        return node;
    }

    private Map<String, Object> leaf(String type, String id, String label, String body, List<Long> sourceIds) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", type);
        node.put("id", id);
        node.put("label", label);
        node.put("body", body);
        node.put("sourceArtifactIds", sourceIds);
        return node;
    }

    private Map<String, Object> artifactNode(Artifact artifact) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "ARTIFACT");
        node.put("id", "artifact-" + artifact.getId());
        node.put("artifactId", artifact.getId());
        node.put("label", artifact.getTitle());
        node.put("status", artifact.getStatus().name());
        node.put("nickname", artifact.getMember().getNickname());
        node.put("school", artifact.getMember().getSchool());
        node.put("major", artifact.getMember().getMajor());
        node.put("tags", parseTags(artifact.getTags()));
        return node;
    }

    private List<Map<String, Object>> parseDiff(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                return objectMapper.convertValue(node, new TypeReference<>() {
                });
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private List<String> parseTags(String json) {
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
}
