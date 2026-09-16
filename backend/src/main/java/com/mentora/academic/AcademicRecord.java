package com.mentora.academic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentora.common.ApiException;
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
@Table(name = "academic_records")
public class AcademicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false)
    private int semester;

    protected AcademicRecord() {
    }

    public AcademicRecord(Student student, String subject, double score, int semester) {
        this.student = student;
        update(subject, score, semester);
    }

    public void update(String subject, double score, int semester) {
        if (subject == null || subject.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        if (subject.trim().length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject must not exceed 100 characters");
        }
        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Score must be between 0 and 100");
        }
        if (semester < 1 || semester > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Semester must be between 1 and 12");
        }
        this.subject = subject.trim();
        this.score = score;
        this.semester = semester;
    }

    public Long getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public double getScore() {
        return score;
    }

    public int getSemester() {
        return semester;
    }
}
