package com.mentora.career;

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
import jakarta.persistence.UniqueConstraint;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Entity
@Table(
        name = "skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_skill_student_normalized_name",
                columnNames = {"student_id", "normalized_name"}
        )
)
public class Skill {

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    protected Skill() {
    }

    public Skill(Student student, String name, Level level) {
        this.student = student;
        rename(name);
        updateLevel(level);
    }

    public void rename(String name) {
        this.normalizedName = normalizeName(name);
        this.name = name.trim();
    }

    public void updateLevel(Level level) {
        if (level == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Skill level is required");
        }
        this.level = level;
    }

    static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Skill name is required");
        }
        String cleanName = name.trim();
        if (cleanName.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Skill name must not exceed 100 characters");
        }
        return cleanName.toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Level getLevel() {
        return level;
    }
}
