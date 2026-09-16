package com.mentora.roadmap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentora.common.ApiException;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.http.HttpStatus;

@Entity
@Table(
        name = "roadmap_steps",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roadmap_step_position",
                columnNames = {"roadmap_id", "position"}
        )
)
public class RoadmapStep {

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    @Column(name = "phase_number", nullable = false)
    private int phaseNumber;

    @Column(name = "phase_title", nullable = false, length = 100)
    private String phaseTitle;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "estimated_hours", nullable = false)
    private int estimatedHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    protected RoadmapStep() {
    }

    RoadmapStep(
            Roadmap roadmap,
            int phaseNumber,
            String phaseTitle,
            int position,
            String title,
            String description,
            int estimatedHours
    ) {
        if (roadmap == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Roadmap is required");
        }
        if (phaseNumber <= 0 || position <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Roadmap phase and position must be positive");
        }
        if (estimatedHours < 1 || estimatedHours > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Estimated hours must be between 1 and 200");
        }
        this.roadmap = roadmap;
        this.phaseNumber = phaseNumber;
        this.phaseTitle = cleanRequired(phaseTitle, 100, "Phase title");
        this.position = position;
        this.title = cleanRequired(title, 150, "Step title");
        this.description = cleanRequired(description, 500, "Step description");
        this.estimatedHours = estimatedHours;
        this.status = Status.NOT_STARTED;
    }

    public void updateStatus(Status status) {
        if (status == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Step status is required");
        }
        this.status = status;
    }

    private String cleanRequired(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, label + " from provider is invalid");
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() > maxLength) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, label + " from provider is too long");
        }
        return clean;
    }

    public Long getId() {
        return id;
    }

    public int getPhaseNumber() {
        return phaseNumber;
    }

    public String getPhaseTitle() {
        return phaseTitle;
    }

    public int getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public Status getStatus() {
        return status;
    }
}
