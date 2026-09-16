package com.mentora.career;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CareerRepository extends JpaRepository<CareerProfile, Long> {

    Optional<CareerProfile> findByStudent(Student student);
}
