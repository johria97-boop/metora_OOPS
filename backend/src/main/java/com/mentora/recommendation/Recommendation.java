package com.mentora.recommendation;

import com.mentora.common.ApiException;
import com.mentora.student.Student;
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
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    public enum Category {
        ACADEMIC, PRODUCTIVITY, CAREER
    }

    public enum Priority {
        URGENT, HIGH, MEDIUM, LOW
    }

    public enum Status {
        ACTIVE, COMPLETED, DISMISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "normalized_title", nullable = false, length = 150)
    private String normalizedTitle;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Recommendation() {
    }

    public Recommendation(
            Student student,
            Category category,
            Priority priority,
            String title,
            String reason
    ) {
        if (student == null || category == null || priority == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recommendation ownership and type are required");
        }
        String cleanTitle = cleanTitle(title);
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recommendation reason is invalid");
        }
        this.student = student;
        this.category = category;
        this.priority = priority;
        this.title = cleanTitle;
        this.normalizedTitle = normalizeTitle(cleanTitle);
        this.reason = reason.trim();
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void markCompleted() {
        requireActive();
        status = Status.COMPLETED;
    }

    public void dismiss() {
        requireActive();
        status = Status.DISMISSED;
    }

    private void requireActive() {
        if (status != Status.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Only active recommendations can change status");
        }
    }

    static String normalizeTitle(String title) {
        return cleanTitle(title).toLowerCase(Locale.ROOT);
    }

    private static String cleanTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recommendation title is required");
        }
        String cleanTitle = title.trim().replaceAll("\\s+", " ");
        if (cleanTitle.length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recommendation title must not exceed 150 characters");
        }
        return cleanTitle;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public String getReason() {
        return reason;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
