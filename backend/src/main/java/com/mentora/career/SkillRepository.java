package com.mentora.career;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByStudentOrderByNameAsc(Student student);

    Optional<Skill> findByIdAndStudent(Long id, Student student);

    boolean existsByStudentAndNormalizedName(Student student, String normalizedName);

    boolean existsByStudentAndNormalizedNameAndIdNot(
            Student student,
            String normalizedName,
            Long id
    );
}
