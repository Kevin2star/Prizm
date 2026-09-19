package com.prizm.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prizm.domain.Artifact;
import com.prizm.domain.ArtifactStatus;
import com.prizm.realtime.SpaceEventPublisher;
import com.prizm.repository.ArtifactRepository;
import com.prizm.service.GroupingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

    private final ArtifactRepository artifactRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final GroupingService groupingService;
    private final SpaceEventPublisher spaceEventPublisher;

    public AnalysisPipeline(
            ArtifactRepository artifactRepository,
            GeminiClient geminiClient,
            ObjectMapper objectMapper,
            GroupingService groupingService,
            SpaceEventPublisher spaceEventPublisher
    ) {
        this.artifactRepository = artifactRepository;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.groupingService = groupingService;
        this.spaceEventPublisher = spaceEventPublisher;
    }

    @Async
    @Transactional
    public void enqueue(Long artifactId) {
        Artifact artifact = artifactRepository.findDetailById(artifactId).orElse(null);
        if (artifact == null) {
            return;
        }
        artifact.setStatus(ArtifactStatus.PROCESSING);
        artifactRepository.save(artifact);
        try {
            if (!geminiClient.isConfigured()) {
                throw new IllegalStateException("GEMINI_API_KEY is not set");
            }
            List<String> tags = geminiClient.generateTags(
                    artifact.getTitle(),
                    artifact.getContent(),
                    artifact.getMember().getSchool(),
                    artifact.getMember().getMajor()
            );
            if (tags.isEmpty()) {
                tags = geminiClient.generateTags(
                        artifact.getTitle(),
                        artifact.getContent(),
                        artifact.getMember().getSchool(),
                        artifact.getMember().getMajor()
                );
            }
            if (tags.isEmpty()) {
                tags = List.of("미분류");
            }
            double[] embedding = geminiClient.embed(artifact.getTitle(), artifact.getContent());
            artifact.setTags(writeJson(tags));
            artifact.setEmbedding(writeJson(embedding));
            artifact.setStatus(ArtifactStatus.READY);
            artifactRepository.save(artifact);
            Long groupId = groupingService.assignGroup(artifact.getId());
            if (groupId != null) {
                spaceEventPublisher.groupsUpdated(artifact.getSpace().getId(), groupId);
            } else {
                spaceEventPublisher.groupsUpdated(artifact.getSpace().getId(), -1L);
            }
        } catch (Exception ex) {
            log.warn("Analysis failed for artifact {}: {}", artifactId, ex.getMessage());
            artifact.setStatus(ArtifactStatus.FAILED);
            artifactRepository.save(artifact);
            spaceEventPublisher.groupsUpdated(artifact.getSpace().getId(), -1L);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
