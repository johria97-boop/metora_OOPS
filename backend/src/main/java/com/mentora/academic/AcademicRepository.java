package com.mentora.academic;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicRepository extends JpaRepository<AcademicRecord, Long> {

    List<AcademicRecord> findAllByStudentOrderBySemesterAscSubjectAsc(Student student);

    Optional<AcademicRecord> findByIdAndStudent(Long id, Student student);
}

