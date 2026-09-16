package com.mentora.roadmap;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    List<Roadmap> findByStudentOrderByCreatedAtDesc(Student student);

    Optional<Roadmap> findByIdAndStudent(Long id, Student student);

    List<Roadmap> findByStudentAndNormalizedCourseName(
            Student student,
            String normalizedCourseName
    );
}
