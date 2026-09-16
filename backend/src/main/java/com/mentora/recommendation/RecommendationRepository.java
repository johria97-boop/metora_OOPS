package com.mentora.recommendation;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findAllByStudentOrderByCreatedAtDesc(Student student);

    List<Recommendation> findAllByStudentAndStatus(
            Student student,
            Recommendation.Status status
    );

    Optional<Recommendation> findByIdAndStudent(Long id, Student student);

    Optional<Recommendation> findByStudentAndCategoryAndNormalizedTitleAndStatus(
            Student student,
            Recommendation.Category category,
            String normalizedTitle,
            Recommendation.Status status
    );
}
