package com.mentora.career;

import com.mentora.common.ApiException;
import com.mentora.student.Student;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "career_profiles")
public class CareerProfile {

    public static final int MAX_INTERESTS = 10;
    public static final int MAX_INTEREST_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "career_goal", nullable = false, length = 150)
    private String careerGoal;

    @ElementCollection
    @CollectionTable(
            name = "career_profile_interests",
            joinColumns = @JoinColumn(name = "career_profile_id")
    )
    @Column(name = "interest", nullable = false, length = MAX_INTEREST_LENGTH)
    private Set<String> interests = new LinkedHashSet<>();

    protected CareerProfile() {
    }

    public CareerProfile(Student student, String careerGoal, List<String> interests) {
        this.student = student;
        update(careerGoal, interests);
    }

    public void update(String careerGoal, List<String> interests) {
        updateCareerGoal(careerGoal);
        replaceInterests(interests);
    }

    public void updateCareerGoal(String careerGoal) {
        if (careerGoal == null || careerGoal.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Career goal is required");
        }
        String cleanGoal = careerGoal.trim();
        if (cleanGoal.length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Career goal must not exceed 150 characters");
        }
        this.careerGoal = cleanGoal;
    }

    public void replaceInterests(List<String> newInterests) {
        if (newInterests == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Interests are required");
        }
        Map<String, String> uniqueInterests = new LinkedHashMap<>();
        for (String interest : newInterests) {
            if (interest == null || interest.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Interests cannot be blank");
            }
            String cleanInterest = interest.trim();
            if (cleanInterest.length() > MAX_INTEREST_LENGTH) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Each interest must not exceed 80 characters");
            }
            uniqueInterests.putIfAbsent(cleanInterest.toLowerCase(Locale.ROOT), cleanInterest);
        }
        if (uniqueInterests.size() > MAX_INTERESTS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No more than 10 interests are allowed");
        }
        interests.clear();
        interests.addAll(uniqueInterests.values());
    }

    public Long getId() {
        return id;
    }

    public String getCareerGoal() {
        return careerGoal;
    }

    public Set<String> getInterests() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(interests));
    }
}
