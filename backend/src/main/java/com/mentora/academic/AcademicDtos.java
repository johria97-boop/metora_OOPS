package com.mentora.academic;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AcademicDtos {

    private AcademicDtos() {
    }

    public record AcademicRecordRequest(
            @NotBlank @Size(max = 100) String subject,
            @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double score,
            @NotNull @Min(1) @Max(12) Integer semester
    ) {
    }

    public record AcademicRecordResponse(Long id, String subject, double score, int semester) {

        public static AcademicRecordResponse from(AcademicRecord record) {
            return new AcademicRecordResponse(
                    record.getId(), record.getSubject(), record.getScore(), record.getSemester()
            );
        }
    }

    public record StudyPlanRequest(
            @NotBlank @Size(max = 100) String subject,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double targetHours,
            @NotNull @DecimalMin("0.0") Double completedHours
    ) {
    }

    public record ProgressRequest(
            @NotNull @DecimalMin("0.0") Double completedHours
    ) {
    }

    public record StudyPlanResponse(
            Long id,
            String subject,
            double targetHours,
            double completedHours,
            double progress
    ) {

        public static StudyPlanResponse from(StudyPlan plan) {
            return new StudyPlanResponse(
                    plan.getId(),
                    plan.getSubject(),
                    plan.getTargetHours(),
                    plan.getCompletedHours(),
                    plan.getProgress()
            );
        }
    }
}

