package com.mentora.productivity;

import com.mentora.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findAllByStudentOrderByEventDateAscTitleAsc(Student student);

    Optional<CalendarEvent> findByIdAndStudent(Long id, Student student);
}
