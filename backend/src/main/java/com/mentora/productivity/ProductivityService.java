package com.mentora.productivity;

import com.mentora.common.ApiException;
import com.mentora.student.Student;
import com.mentora.student.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductivityService {

    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final StudentService studentService;

    public ProductivityService(
            TaskRepository taskRepository,
            HabitRepository habitRepository,
            CalendarEventRepository calendarEventRepository,
            StudentService studentService
    ) {
        this.taskRepository = taskRepository;
        this.habitRepository = habitRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public List<ProductivityDtos.TaskResponse> getTasks(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return taskRepository.findAllByStudentOrderByCompletedAscDueDateAsc(student)
                .stream().map(ProductivityDtos.TaskResponse::from).toList();
    }

    @Transactional
    public ProductivityDtos.TaskResponse createTask(
            String email,
            ProductivityDtos.TaskCreateRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        Task task = new Task(student, request.title(), request.dueDate(), request.priority());
        return ProductivityDtos.TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public ProductivityDtos.TaskResponse updateTask(
            String email,
            Long id,
            ProductivityDtos.TaskUpdateRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        Task task = requireTask(id, student);
        task.update(request.title(), request.priority());
        return ProductivityDtos.TaskResponse.from(task);
    }

    @Transactional
    public ProductivityDtos.TaskResponse completeTask(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        Task task = requireTask(id, student);
        task.markCompleted();
        return ProductivityDtos.TaskResponse.from(task);
    }

    @Transactional
    public ProductivityDtos.TaskResponse rescheduleTask(
            String email,
            Long id,
            ProductivityDtos.RescheduleRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        Task task = requireTask(id, student);
        task.reschedule(request.scheduledDate());
        return ProductivityDtos.TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        taskRepository.delete(requireTask(id, student));
    }

    @Transactional(readOnly = true)
    public List<ProductivityDtos.HabitResponse> getHabits(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return habitRepository.findAllByStudentOrderByNameAsc(student)
                .stream().map(ProductivityDtos.HabitResponse::from).toList();
    }

    @Transactional
    public ProductivityDtos.HabitResponse createHabit(
            String email,
            ProductivityDtos.HabitRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        Habit habit = new Habit(student, request.name(), request.targetStreak());
        return ProductivityDtos.HabitResponse.from(habitRepository.save(habit));
    }

    @Transactional
    public ProductivityDtos.HabitResponse updateHabit(
            String email,
            Long id,
            ProductivityDtos.HabitRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        Habit habit = requireHabit(id, student);
        habit.update(request.name(), request.targetStreak());
        return ProductivityDtos.HabitResponse.from(habit);
    }

    @Transactional
    public ProductivityDtos.HabitResponse completeHabit(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        Habit habit = requireHabit(id, student);
        habit.recordCompletion(LocalDate.now());
        return ProductivityDtos.HabitResponse.from(habit);
    }

    @Transactional
    public void deleteHabit(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        habitRepository.delete(requireHabit(id, student));
    }

    @Transactional(readOnly = true)
    public List<ProductivityDtos.CalendarEventResponse> getCalendarEvents(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return calendarEventRepository.findAllByStudentOrderByEventDateAscTitleAsc(student)
                .stream().map(ProductivityDtos.CalendarEventResponse::from).toList();
    }

    @Transactional
    public ProductivityDtos.CalendarEventResponse createCalendarEvent(
            String email,
            ProductivityDtos.CalendarEventCreateRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        CalendarEvent event = new CalendarEvent(student, request.title(), request.eventDate());
        return ProductivityDtos.CalendarEventResponse.from(calendarEventRepository.save(event));
    }

    @Transactional
    public ProductivityDtos.CalendarEventResponse updateCalendarEvent(
            String email,
            Long id,
            ProductivityDtos.CalendarEventUpdateRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        CalendarEvent event = requireCalendarEvent(id, student);
        event.updateTitle(request.title());
        return ProductivityDtos.CalendarEventResponse.from(event);
    }

    @Transactional
    public ProductivityDtos.CalendarEventResponse rescheduleCalendarEvent(
            String email,
            Long id,
            ProductivityDtos.RescheduleRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        CalendarEvent event = requireCalendarEvent(id, student);
        event.reschedule(request.scheduledDate());
        return ProductivityDtos.CalendarEventResponse.from(event);
    }

    @Transactional
    public void deleteCalendarEvent(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        calendarEventRepository.delete(requireCalendarEvent(id, student));
    }

    private Task requireTask(Long id, Student student) {
        return taskRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Habit requireHabit(Long id, Student student) {
        return habitRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Habit not found"));
    }

    private CalendarEvent requireCalendarEvent(Long id, Student student) {
        return calendarEventRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Calendar event not found"));
    }
}
