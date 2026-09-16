package com.mentora.productivity;

import com.mentora.common.ApiException;
import com.mentora.common.Schedulable;
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

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task implements Schedulable {

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(nullable = false)
    private boolean completed;

    protected Task() {
    }

    public Task(Student student, String title, LocalDate dueDate, Priority priority) {
        this.student = student;
        update(title, priority);
        reschedule(dueDate);
        this.completed = false;
    }

    public void update(String title, Priority priority) {
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Task title is required");
        }
        if (title.trim().length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Task title must not exceed 150 characters");
        }
        if (priority == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Task priority is required");
        }
        this.title = title.trim();
        this.priority = priority;
    }

    public void markCompleted() {
        this.completed = true;
    }

    @Override
    public LocalDate getScheduledDate() {
        return dueDate;
    }

    @Override
    public void reschedule(LocalDate newDate) {
        if (newDate == null || newDate.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Task due date cannot be in the past");
        }
        this.dueDate = newDate;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }
}
