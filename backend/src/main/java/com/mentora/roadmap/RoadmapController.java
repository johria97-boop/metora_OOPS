package com.mentora.roadmap;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @PostMapping("/generate")
    public ResponseEntity<RoadmapDtos.RoadmapResponse> generate(
            Authentication authentication,
            @Valid @RequestBody RoadmapDtos.GenerateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roadmapService.generate(authentication.getName(), request));
    }

    @GetMapping
    public List<RoadmapDtos.RoadmapResponse> getRoadmaps(Authentication authentication) {
        return roadmapService.getRoadmaps(authentication.getName());
    }

    @GetMapping("/{id}")
    public RoadmapDtos.RoadmapResponse getRoadmap(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return roadmapService.getRoadmap(authentication.getName(), id);
    }

    @PatchMapping("/{roadmapId}/steps/{stepId}/status")
    public RoadmapDtos.RoadmapResponse updateStepStatus(
            Authentication authentication,
            @PathVariable Long roadmapId,
            @PathVariable Long stepId,
            @Valid @RequestBody RoadmapDtos.StatusRequest request
    ) {
        return roadmapService.updateStepStatus(
                authentication.getName(), roadmapId, stepId, request
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(
            Authentication authentication,
            @PathVariable Long id
    ) {
        roadmapService.deleteRoadmap(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
