package com.prizm.api;

import com.prizm.api.dto.ArtifactResponse;
import com.prizm.api.dto.CreateArtifactRequest;
import com.prizm.service.ArtifactService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArtifactController {

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @PostMapping("/api/spaces/{spaceId}/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtifactResponse create(@PathVariable Long spaceId, @RequestBody CreateArtifactRequest request) {
        return artifactService.create(
                spaceId,
                request == null ? null : request.memberId(),
                request == null ? null : request.title(),
                request == null ? null : request.content()
        );
    }

    @GetMapping("/api/spaces/{spaceId}/artifacts")
    public List<ArtifactResponse> list(@PathVariable Long spaceId) {
        return artifactService.list(spaceId);
    }

    @GetMapping("/api/artifacts/{id}")
    public ArtifactResponse get(@PathVariable Long id) {
        return artifactService.get(id);
    }
}
