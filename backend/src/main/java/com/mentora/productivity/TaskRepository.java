package com.mentora.productivity;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByStudentOrderByCompletedAscDueDateAsc(Student student);

    List<Task> findAllByStudentAndCompletedFalseOrderByDueDateAsc(Student student);

    Optional<Task> findByIdAndStudent(Long id, Student student);
}
