package com.prizm.api;

import com.prizm.api.dto.CreateSpaceRequest;
import com.prizm.api.dto.JoinSpaceRequest;
import com.prizm.api.dto.JoinSpaceResponse;
import com.prizm.api.dto.SpaceDetailResponse;
import com.prizm.api.dto.SpaceResponse;
import com.prizm.service.SpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @PostMapping
    public SpaceResponse create(@RequestBody CreateSpaceRequest request) {
        return spaceService.create(request == null ? null : request.name());
    }

    @PostMapping("/{code}/join")
    public JoinSpaceResponse join(@PathVariable String code, @RequestBody JoinSpaceRequest request) {
        return spaceService.join(
                code,
                request == null ? null : request.nickname(),
                request == null ? null : request.school(),
                request == null ? null : request.major()
        );
    }

    @GetMapping("/{id}")
    public SpaceDetailResponse get(@PathVariable Long id) {
        return spaceService.get(id);
    }
}
