package com.mentora.academic;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    List<StudyPlan> findAllByStudentOrderBySubjectAsc(Student student);

    Optional<StudyPlan> findByIdAndStudent(Long id, Student student);
}

