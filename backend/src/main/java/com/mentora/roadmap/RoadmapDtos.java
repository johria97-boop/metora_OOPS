package com.mentora.roadmap;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class RoadmapDtos {

    private RoadmapDtos() {
    }

    public record GenerateRequest(
            @NotBlank @Size(max = 120) String courseName,
            Roadmap.Level currentLevel,
            @Min(1) @Max(52) Integer targetWeeks
    ) {
    }

    public record StatusRequest(@NotNull RoadmapStep.Status status) {
    }

    public record StepResponse(
            Long id,
            int phaseNumber,
            String phaseTitle,
            int position,
            String title,
            String description,
            int estimatedHours,
            RoadmapStep.Status status
    ) {

        static StepResponse from(RoadmapStep step) {
            return new StepResponse(
                    step.getId(), step.getPhaseNumber(), step.getPhaseTitle(),
                    step.getPosition(), step.getTitle(), step.getDescription(),
                    step.getEstimatedHours(), step.getStatus()
            );
        }
    }

    public record RoadmapResponse(
            Long id,
            String courseName,
            String summary,
            Roadmap.Level currentLevel,
            Integer targetWeeks,
            double progress,
            String state,
            int completedSteps,
            int totalSteps,
            Instant createdAt,
            List<StepResponse> steps
    ) {

        public static RoadmapResponse from(Roadmap roadmap) {
            return new RoadmapResponse(
                    roadmap.getId(),
                    roadmap.getCourseName(),
                    roadmap.getSummary(),
                    roadmap.getCurrentLevel(),
                    roadmap.getTargetWeeks(),
                    roadmap.getProgress(),
                    roadmap.isCompleted() ? "COMPLETED" : "ACTIVE",
                    roadmap.getCompletedStepCount(),
                    roadmap.getSteps().size(),
                    roadmap.getCreatedAt(),
                    roadmap.getSteps().stream().map(StepResponse::from).toList()
            );
        }
    }
}
