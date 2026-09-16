package com.mentora.recommendation;

import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.career.CareerProfile;
import com.mentora.career.CareerRepository;
import com.mentora.career.SkillRepository;
import com.mentora.common.ApiException;
import com.mentora.productivity.CalendarEventRepository;
import com.mentora.productivity.HabitRepository;
import com.mentora.productivity.TaskRepository;
import com.mentora.student.Student;
import com.mentora.student.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationService.class);
    private static final int MAX_RECOMMENDATIONS = 5;

    private final RecommendationRepository recommendationRepository;
    private final RecommendationProvider recommendationProvider;
    private final StudentService studentService;
    private final AcademicRepository academicRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CareerRepository careerRepository;
    private final SkillRepository skillRepository;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            RecommendationProvider recommendationProvider,
            StudentService studentService,
            AcademicRepository academicRepository,
            StudyPlanRepository studyPlanRepository,
            TaskRepository taskRepository,
            HabitRepository habitRepository,
            CalendarEventRepository calendarEventRepository,
            CareerRepository careerRepository,
            SkillRepository skillRepository
    ) {
        this.recommendationRepository = recommendationRepository;
        this.recommendationProvider = recommendationProvider;
        this.studentService = studentService;
        this.academicRepository = academicRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.taskRepository = taskRepository;
        this.habitRepository = habitRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.careerRepository = careerRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public List<RecommendationDtos.RecommendationResponse> generate(String email) {
        Student student = studentService.requireCurrentStudent(email);
        RecommendationProvider.StudentContext context = buildContext(student);
        if (!hasMeaningfulContext(context)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Add academic, productivity, or career information before generating recommendations");
        }

        List<RecommendationProvider.GeneratedRecommendation> providerOutput;
        try {
            providerOutput = recommendationProvider.generate(context);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.warn("Recommendation provider failed");
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Recommendation provider is temporarily unavailable");
        }
        if (providerOutput == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Recommendation provider returned an invalid response");
        }

        List<RecommendationDtos.RecommendationResponse> accepted = new ArrayList<>();
        Set<String> generationKeys = new HashSet<>();
        for (RecommendationProvider.GeneratedRecommendation item : providerOutput) {
            if (accepted.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
            Recommendation recommendation = validatedRecommendation(student, item);
            if (recommendation == null) {
                continue;
            }
            String key = recommendation.getCategory() + ":" + recommendation.getNormalizedTitle();
            if (!generationKeys.add(key)) {
                continue;
            }
            Recommendation existing = recommendationRepository
                    .findByStudentAndCategoryAndNormalizedTitleAndStatus(
                            student,
                            recommendation.getCategory(),
                            recommendation.getNormalizedTitle(),
                            Recommendation.Status.ACTIVE
                    )
                    .orElse(null);
            Recommendation resolved = existing == null
                    ? recommendationRepository.save(recommendation)
                    : existing;
            accepted.add(RecommendationDtos.RecommendationResponse.from(resolved));
        }
        LOGGER.info("Accepted {} recommendation entries", accepted.size());
        return accepted;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDtos.RecommendationResponse> getRecommendations(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return recommendationRepository.findAllByStudentOrderByCreatedAtDesc(student)
                .stream().map(RecommendationDtos.RecommendationResponse::from).toList();
    }

    @Transactional
    public RecommendationDtos.RecommendationResponse complete(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        Recommendation recommendation = requireRecommendation(id, student);
        recommendation.markCompleted();
        return RecommendationDtos.RecommendationResponse.from(recommendation);
    }

    @Transactional
    public RecommendationDtos.RecommendationResponse dismiss(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        Recommendation recommendation = requireRecommendation(id, student);
        recommendation.dismiss();
        return RecommendationDtos.RecommendationResponse.from(recommendation);
    }

    private RecommendationProvider.StudentContext buildContext(Student student) {
        List<RecommendationProvider.AcademicItem> academics = academicRepository
                .findAllByStudentOrderBySemesterAscSubjectAsc(student)
                .stream().map(record -> new RecommendationProvider.AcademicItem(
                        record.getSubject(), record.getScore(), record.getSemester()
                )).toList();
        List<RecommendationProvider.StudyPlanItem> plans = studyPlanRepository
                .findAllByStudentOrderBySubjectAsc(student)
                .stream().map(plan -> new RecommendationProvider.StudyPlanItem(
                        plan.getSubject(), plan.getTargetHours(),
                        plan.getCompletedHours(), plan.getProgress()
                )).toList();
        List<RecommendationProvider.TaskItem> tasks = taskRepository
                .findAllByStudentAndCompletedFalseOrderByDueDateAsc(student)
                .stream().map(task -> new RecommendationProvider.TaskItem(
                        task.getTitle(), task.getDueDate(), task.getPriority().name()
                )).toList();
        List<RecommendationProvider.HabitItem> habits = habitRepository
                .findAllByStudentOrderByNameAsc(student)
                .stream().map(habit -> new RecommendationProvider.HabitItem(
                        habit.getName(), habit.getTargetStreak(),
                        habit.getCurrentStreak(), habit.getProgress()
                )).toList();
        List<RecommendationProvider.CalendarItem> events = calendarEventRepository
                .findAllByStudentOrderByEventDateAscTitleAsc(student)
                .stream().map(event -> new RecommendationProvider.CalendarItem(
                        event.getTitle(), event.getEventDate()
                )).toList();
        CareerProfile profile = careerRepository.findByStudent(student).orElse(null);
        RecommendationProvider.CareerItem career = profile == null
                ? new RecommendationProvider.CareerItem("", List.of())
                : new RecommendationProvider.CareerItem(
                        profile.getCareerGoal(), List.copyOf(profile.getInterests())
                );
        List<RecommendationProvider.SkillItem> skills = skillRepository
                .findAllByStudentOrderByNameAsc(student)
                .stream().map(skill -> new RecommendationProvider.SkillItem(
                        skill.getName(), skill.getLevel().name()
                )).toList();

        return new RecommendationProvider.StudentContext(
                student.getSemester(), student.getBranch(), academics, plans,
                tasks, habits, events, career, skills
        );
    }

    private boolean hasMeaningfulContext(RecommendationProvider.StudentContext context) {
        return !context.academicRecords().isEmpty()
                || !context.studyPlans().isEmpty()
                || !context.pendingTasks().isEmpty()
                || !context.habits().isEmpty()
                || !context.calendarEvents().isEmpty()
                || !context.career().careerGoal().isBlank()
                || !context.career().interests().isEmpty()
                || !context.skills().isEmpty();
    }

    private Recommendation validatedRecommendation(
            Student student,
            RecommendationProvider.GeneratedRecommendation item
    ) {
        if (item == null) {
            return null;
        }
        try {
            Recommendation.Category category = Recommendation.Category.valueOf(
                    item.category().trim().toUpperCase(Locale.ROOT)
            );
            Recommendation.Priority priority = Recommendation.Priority.valueOf(
                    item.priority().trim().toUpperCase(Locale.ROOT)
            );
            return new Recommendation(student, category, priority, item.title(), item.reason());
        } catch (ApiException | IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    private Recommendation requireRecommendation(Long id, Student student) {
        return recommendationRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recommendation not found"));
    }
}
