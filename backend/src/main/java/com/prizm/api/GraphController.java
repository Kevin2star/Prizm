package com.prizm.api;

import com.prizm.service.GraphService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/api/spaces/{id}/graph")
    public Map<String, Object> graph(@PathVariable Long id) {
        return graphService.getGraph(id);
    }
}
