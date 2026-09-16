package com.mentora.academic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentora.common.ApiException;
import com.mentora.common.Trackable;
import com.mentora.student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "study_plans")
public class StudyPlan implements Trackable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(name = "target_hours", nullable = false)
    private double targetHours;

    @Column(name = "completed_hours", nullable = false)
    private double completedHours;

    protected StudyPlan() {
    }

    public StudyPlan(Student student, String subject, double targetHours, double completedHours) {
        this.student = student;
        update(subject, targetHours, completedHours);
    }

    public void update(String subject, double targetHours, double completedHours) {
        if (subject == null || subject.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        if (subject.trim().length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject must not exceed 100 characters");
        }
        if (!Double.isFinite(targetHours) || targetHours <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target hours must be greater than zero");
        }
        validateCompletedHours(completedHours, targetHours);
        this.subject = subject.trim();
        this.targetHours = targetHours;
        this.completedHours = completedHours;
    }

    public void updateProgress(double completedHours) {
        validateCompletedHours(completedHours, targetHours);
        this.completedHours = completedHours;
    }

    @Override
    public double getProgress() {
        return completedHours / targetHours * 100.0;
    }

    private void validateCompletedHours(double completedHours, double targetHours) {
        if (!Double.isFinite(completedHours) || completedHours < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completed hours cannot be negative");
        }
        if (completedHours > targetHours) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completed hours cannot exceed target hours");
        }
    }

    public Long getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public double getTargetHours() {
        return targetHours;
    }

    public double getCompletedHours() {
        return completedHours;
    }
}
