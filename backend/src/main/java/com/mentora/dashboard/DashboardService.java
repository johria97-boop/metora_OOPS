package com.mentora.dashboard;

import com.mentora.academic.AcademicRecord;
import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlan;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.career.CareerProfile;
import com.mentora.career.CareerRepository;
import com.mentora.career.Skill;
import com.mentora.career.SkillRepository;
import com.mentora.productivity.CalendarEvent;
import com.mentora.productivity.CalendarEventRepository;
import com.mentora.productivity.Habit;
import com.mentora.productivity.HabitRepository;
import com.mentora.productivity.Task;
import com.mentora.productivity.TaskRepository;
import com.mentora.recommendation.Recommendation;
import com.mentora.recommendation.RecommendationRepository;
import com.mentora.roadmap.Roadmap;
import com.mentora.roadmap.RoadmapRepository;
import com.mentora.student.Student;
import com.mentora.student.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private static final int LIST_PREVIEW_LIMIT = 5;
    private static final int RECOMMENDATION_PREVIEW_LIMIT = 3;

    private final StudentService studentService;
    private final AcademicRepository academicRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CareerRepository careerRepository;
    private final SkillRepository skillRepository;
    private final RecommendationRepository recommendationRepository;
    private final RoadmapRepository roadmapRepository;

    public DashboardService(
            StudentService studentService,
            AcademicRepository academicRepository,
            StudyPlanRepository studyPlanRepository,
            TaskRepository taskRepository,
            HabitRepository habitRepository,
            CalendarEventRepository calendarEventRepository,
            CareerRepository careerRepository,
            SkillRepository skillRepository,
            RecommendationRepository recommendationRepository,
            RoadmapRepository roadmapRepository
    ) {
        this.studentService = studentService;
        this.academicRepository = academicRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.taskRepository = taskRepository;
        this.habitRepository = habitRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.careerRepository = careerRepository;
        this.skillRepository = skillRepository;
        this.recommendationRepository = recommendationRepository;
        this.roadmapRepository = roadmapRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardResponse getDashboard(String authenticatedEmail) {
        Student student = studentService.requireCurrentStudent(authenticatedEmail);
        return new DashboardDtos.DashboardResponse(
                new DashboardDtos.StudentSummary(
                        student.getName(), student.getBranch(), student.getSemester()
                ),
                academicSummary(student),
                studyPlanSummary(student),
                taskSummary(student),
                habitSummary(student),
                calendarSummary(student),
                careerSummary(student),
                recommendationSummary(student),
                roadmapSummary(student)
        );
    }

    private DashboardDtos.AcademicSummary academicSummary(Student student) {
        List<AcademicRecord> records = academicRepository
                .findAllByStudentOrderBySemesterAscSubjectAsc(student);
        if (records.isEmpty()) {
            return new DashboardDtos.AcademicSummary(0, null, null, null, List.of());
        }
        Comparator<AcademicRecord> scoreOrder = Comparator
                .comparingDouble(AcademicRecord::getScore)
                .thenComparing(AcademicRecord::getSubject, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(AcademicRecord::getSemester);
        AcademicRecord strongest = records.stream().max(scoreOrder).orElseThrow();
        AcademicRecord attention = records.stream().min(scoreOrder).orElseThrow();
        double average = records.stream().mapToDouble(AcademicRecord::getScore).average().orElseThrow();
        List<DashboardDtos.SubjectScore> subjects = records.stream()
                .map(this::subjectScore).toList();
        return new DashboardDtos.AcademicSummary(
                records.size(), average, subjectScore(strongest), subjectScore(attention), subjects
        );
    }

    private DashboardDtos.SubjectScore subjectScore(AcademicRecord record) {
        return new DashboardDtos.SubjectScore(
                record.getSubject(), record.getScore(), record.getSemester()
        );
    }

    private DashboardDtos.StudyPlanSummary studyPlanSummary(Student student) {
        List<StudyPlan> plans = studyPlanRepository.findAllByStudentOrderBySubjectAsc(student);
        Double averageProgress = plans.isEmpty() ? null : plans.stream()
                .mapToDouble(StudyPlan::getProgress).average().orElseThrow();
        List<DashboardDtos.StudyPlanItem> preview = plans.stream()
                .limit(LIST_PREVIEW_LIMIT)
                .map(plan -> new DashboardDtos.StudyPlanItem(
                        plan.getSubject(), plan.getTargetHours(),
                        plan.getCompletedHours(), plan.getProgress()
                )).toList();
        return new DashboardDtos.StudyPlanSummary(plans.size(), averageProgress, preview);
    }

    private DashboardDtos.TaskSummary taskSummary(Student student) {
        List<Task> pending = new ArrayList<>(taskRepository
                .findAllByStudentAndCompletedFalseOrderByDueDateAsc(student));
        pending.sort(Comparator.comparing(Task::getDueDate)
                .thenComparingInt(task -> taskPriorityOrder(task.getPriority()))
                .thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER));
        int highPriorityCount = (int) pending.stream()
                .filter(task -> task.getPriority() == Task.Priority.HIGH).count();
        List<DashboardDtos.TaskItem> preview = pending.stream()
                .limit(LIST_PREVIEW_LIMIT)
                .map(task -> new DashboardDtos.TaskItem(
                        task.getTitle(), task.getDueDate(), task.getPriority()
                )).toList();
        return new DashboardDtos.TaskSummary(pending.size(), highPriorityCount, preview);
    }

    private int taskPriorityOrder(Task.Priority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    private DashboardDtos.HabitSummary habitSummary(Student student) {
        List<Habit> habits = habitRepository.findAllByStudentOrderByNameAsc(student);
        List<DashboardDtos.HabitItem> preview = habits.stream()
                .limit(LIST_PREVIEW_LIMIT)
                .map(habit -> new DashboardDtos.HabitItem(
                        habit.getName(), habit.getTargetStreak(),
                        habit.getCurrentStreak(), habit.getProgress()
                )).toList();
        return new DashboardDtos.HabitSummary(habits.size(), preview);
    }

    private DashboardDtos.CalendarSummary calendarSummary(Student student) {
        List<CalendarEvent> upcoming = calendarEventRepository
                .findAllByStudentOrderByEventDateAscTitleAsc(student)
                .stream()
                .filter(event -> !event.getEventDate().isBefore(LocalDate.now()))
                .toList();
        List<DashboardDtos.CalendarItem> preview = upcoming.stream()
                .limit(LIST_PREVIEW_LIMIT)
                .map(event -> new DashboardDtos.CalendarItem(
                        event.getTitle(), event.getEventDate()
                )).toList();
        return new DashboardDtos.CalendarSummary(upcoming.size(), preview);
    }

    private DashboardDtos.CareerSummary careerSummary(Student student) {
        CareerProfile profile = careerRepository.findByStudent(student).orElse(null);
        List<Skill> skills = skillRepository.findAllByStudentOrderByNameAsc(student);
        List<DashboardDtos.SkillItem> preview = skills.stream()
                .limit(LIST_PREVIEW_LIMIT)
                .map(skill -> new DashboardDtos.SkillItem(skill.getName(), skill.getLevel()))
                .toList();
        return new DashboardDtos.CareerSummary(
                profile == null,
                profile == null ? "" : profile.getCareerGoal(),
                profile == null ? List.of() : List.copyOf(profile.getInterests()),
                skills.size(),
                preview
        );
    }

    private DashboardDtos.RecommendationSummary recommendationSummary(Student student) {
        List<Recommendation> active = new ArrayList<>(recommendationRepository
                .findAllByStudentAndStatus(student, Recommendation.Status.ACTIVE));
        active.sort(Comparator
                .comparingInt((Recommendation item) -> recommendationPriorityOrder(item.getPriority()))
                .thenComparing(Recommendation::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(Recommendation::getTitle, String.CASE_INSENSITIVE_ORDER));
        List<DashboardDtos.RecommendationItem> preview = active.stream()
                .limit(RECOMMENDATION_PREVIEW_LIMIT)
                .map(item -> new DashboardDtos.RecommendationItem(
                        item.getCategory(), item.getPriority(), item.getTitle(),
                        item.getReason(), item.getCreatedAt()
                )).toList();
        return new DashboardDtos.RecommendationSummary(active.size(), preview);
    }

    private int recommendationPriorityOrder(Recommendation.Priority priority) {
        return switch (priority) {
            case URGENT -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private DashboardDtos.RoadmapSummary roadmapSummary(Student student) {
        List<Roadmap> roadmaps = roadmapRepository.findByStudentOrderByCreatedAtDesc(student);
        List<Roadmap> active = roadmaps.stream().filter(item -> !item.isCompleted()).toList();
        Roadmap current = active.isEmpty()
                ? roadmaps.stream().findFirst().orElse(null)
                : active.getFirst();
        DashboardDtos.RoadmapItem item = current == null ? null : new DashboardDtos.RoadmapItem(
                current.getId(), current.getCourseName(), current.getProgress(),
                current.getCompletedStepCount(), current.getSteps().size()
        );
        return new DashboardDtos.RoadmapSummary(roadmaps.size(), active.size(), item);
    }
}
