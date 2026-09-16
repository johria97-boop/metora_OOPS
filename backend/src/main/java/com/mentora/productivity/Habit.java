package com.mentora.productivity;

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

import java.time.LocalDate;

@Entity
@Table(name = "habits")
public class Habit implements Trackable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "target_streak", nullable = false)
    private int targetStreak;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    protected Habit() {
    }

    public Habit(Student student, String name, int targetStreak) {
        this.student = student;
        update(name, targetStreak);
        this.currentStreak = 0;
    }

    public void update(String name, int targetStreak) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Habit name is required");
        }
        if (name.trim().length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Habit name must not exceed 100 characters");
        }
        if (targetStreak <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target streak must be greater than zero");
        }
        this.name = name.trim();
        this.targetStreak = targetStreak;
    }

    public void recordCompletion(LocalDate completionDate) {
        if (completionDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completion date is required");
        }
        if (lastCompletedDate == null) {
            currentStreak = 1;
        } else if (completionDate.isEqual(lastCompletedDate)) {
            return;
        } else if (completionDate.isBefore(lastCompletedDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completion date cannot precede the last completion");
        } else if (completionDate.isEqual(lastCompletedDate.plusDays(1))) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }
        lastCompletedDate = completionDate;
    }

    @Override
    public double getProgress() {
        return Math.min(100.0, currentStreak * 100.0 / targetStreak);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTargetStreak() {
        return targetStreak;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public LocalDate getLastCompletedDate() {
        return lastCompletedDate;
    }
}
