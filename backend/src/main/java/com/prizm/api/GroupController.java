package com.prizm.api;

import com.prizm.service.GroupQueryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {

    private final GroupQueryService groupQueryService;

    public GroupController(GroupQueryService groupQueryService) {
        this.groupQueryService = groupQueryService;
    }

    @GetMapping("/api/groups/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return groupQueryService.get(id);
    }
}
