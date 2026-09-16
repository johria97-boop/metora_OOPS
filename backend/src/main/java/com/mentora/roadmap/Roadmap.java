package com.mentora.roadmap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentora.common.ApiException;
import com.mentora.common.Trackable;
import com.mentora.student.Student;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "roadmaps")
public class Roadmap implements Trackable {

    public enum Level {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "course_name", nullable = false, length = 120)
    private String courseName;

    @JsonIgnore
    @Column(name = "normalized_course_name", nullable = false, length = 120)
    private String normalizedCourseName;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false, length = 20)
    private Level currentLevel;

    @Column(name = "target_weeks")
    private Integer targetWeeks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RoadmapStep> steps = new ArrayList<>();

    protected Roadmap() {
    }

    public Roadmap(
            Student student,
            String courseName,
            String summary,
            Level currentLevel,
            Integer targetWeeks
    ) {
        if (student == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Student is required");
        }
        String cleanCourseName = cleanRequired(courseName, 120, "Course name");
        this.student = student;
        this.courseName = cleanCourseName;
        this.normalizedCourseName = normalizeCourseName(cleanCourseName);
        this.summary = cleanRequired(summary, 1000, "Roadmap summary");
        this.currentLevel = currentLevel == null ? Level.BEGINNER : currentLevel;
        if (targetWeeks != null && (targetWeeks < 1 || targetWeeks > 52)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target weeks must be between 1 and 52");
        }
        this.targetWeeks = targetWeeks;
        this.createdAt = Instant.now();
    }

    public void addStep(
            int phaseNumber,
            String phaseTitle,
            int position,
            String title,
            String description,
            int estimatedHours
    ) {
        steps.add(new RoadmapStep(
                this, phaseNumber, phaseTitle, position, title, description, estimatedHours
        ));
    }

    @Override
    public double getProgress() {
        if (steps.isEmpty()) {
            return 0.0;
        }
        return Math.min(100.0, getCompletedStepCount() * 100.0 / steps.size());
    }

    public int getCompletedStepCount() {
        return (int) steps.stream()
                .filter(step -> step.getStatus() == RoadmapStep.Status.COMPLETED)
                .count();
    }

    public boolean isCompleted() {
        return !steps.isEmpty() && getCompletedStepCount() == steps.size();
    }

    public RoadmapStep requireStep(Long stepId) {
        return steps.stream()
                .filter(step -> step.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Roadmap step not found"));
    }

    public static String normalizeCourseName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String cleanRequired(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() > maxLength) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    label + " must not exceed " + maxLength + " characters");
        }
        return clean;
    }

    public Long getId() {
        return id;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getNormalizedCourseName() {
        return normalizedCourseName;
    }

    public String getSummary() {
        return summary;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public Integer getTargetWeeks() {
        return targetWeeks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<RoadmapStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
