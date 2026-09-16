package com.mentora.productivity;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findAllByStudentOrderByNameAsc(Student student);

    Optional<Habit> findByIdAndStudent(Long id, Student student);
}
