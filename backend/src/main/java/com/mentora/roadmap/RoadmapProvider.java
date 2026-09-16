package com.mentora.roadmap;

import java.util.List;

public interface RoadmapProvider {

    GeneratedRoadmap generate(RoadmapRequest request);

    record RoadmapRequest(
            String courseName,
            Roadmap.Level currentLevel,
            Integer targetWeeks
    ) {
    }

    record GeneratedRoadmap(String summary, List<GeneratedStep> steps) {
    }

    record GeneratedStep(
            Integer phaseNumber,
            String phaseTitle,
            Integer position,
            String title,
            String description,
            Integer estimatedHours
    ) {
    }
}
