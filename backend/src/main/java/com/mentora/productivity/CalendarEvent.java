package com.mentora.productivity;

import com.mentora.common.ApiException;
import com.mentora.common.Schedulable;
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
@Table(name = "calendar_events")
public class CalendarEvent implements Schedulable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    protected CalendarEvent() {
    }

    public CalendarEvent(Student student, String title, LocalDate eventDate) {
        this.student = student;
        updateTitle(title);
        reschedule(eventDate);
    }

    public void updateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event title is required");
        }
        if (title.trim().length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event title must not exceed 150 characters");
        }
        this.title = title.trim();
    }

    @Override
    public LocalDate getScheduledDate() {
        return eventDate;
    }

    @Override
    public void reschedule(LocalDate newDate) {
        if (newDate == null || newDate.isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event date cannot be in the past");
        }
        this.eventDate = newDate;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }
}
