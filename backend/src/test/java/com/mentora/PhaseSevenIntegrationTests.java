package com.mentora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.academic.AcademicRecord;
import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlan;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.auth.UserRepository;
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
import com.mentora.recommendation.RecommendationProvider;
import com.mentora.recommendation.RecommendationRepository;
import com.mentora.student.Student;
import com.mentora.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PhaseSevenIntegrationTests.ProviderTestConfig.class)
class PhaseSevenIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CountingRecommendationProvider provider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private AcademicRepository academicRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        provider.calls = 0;
        recommendationRepository.deleteAll();
        skillRepository.deleteAll();
        careerRepository.deleteAll();
        calendarEventRepository.deleteAll();
        habitRepository.deleteAll();
        taskRepository.deleteAll();
        studyPlanRepository.deleteAll();
        academicRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedDashboardRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newStudentReceivesOwnedIdentityAndClearEmptySummaries() throws Exception {
        String token = register("empty-dashboard@example.com", "New Student", "Information Technology", 2);

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.name").value("New Student"))
                .andExpect(jsonPath("$.student.branch").value("Information Technology"))
                .andExpect(jsonPath("$.student.semester").value(2))
                .andExpect(jsonPath("$.student.id").doesNotExist())
                .andExpect(jsonPath("$.student.email").doesNotExist())
                .andExpect(jsonPath("$.academics.subjectCount").value(0))
                .andExpect(jsonPath("$.academics.averageScore").value((Object) null))
                .andExpect(jsonPath("$.academics.strongestSubject").value((Object) null))
                .andExpect(jsonPath("$.academics.attentionSubject").value((Object) null))
                .andExpect(jsonPath("$.studyPlans.totalPlans").value(0))
                .andExpect(jsonPath("$.studyPlans.averageProgress").value((Object) null))
                .andExpect(jsonPath("$.tasks.pendingTaskCount").value(0))
                .andExpect(jsonPath("$.habits.totalHabits").value(0))
                .andExpect(jsonPath("$.calendar.upcomingEventCount").value(0))
                .andExpect(jsonPath("$.career.setupNeeded").value(true))
                .andExpect(jsonPath("$.career.skillCount").value(0))
                .andExpect(jsonPath("$.recommendations.activeCount").value(0));

        assertEquals(0, provider.calls);
    }

    @Test
    void dashboardAggregatesDomainProgressCountsAndOrderedLimitedPreviews() throws Exception {
        String email = "full-dashboard@example.com";
        String token = register(email, "Dashboard Student", "CSE", 5);
        Student student = student(email);

        academicRepository.saveAll(List.of(
                new AcademicRecord(student, "Mathematics", 40, 5),
                new AcademicRecord(student, "Databases", 70, 5),
                new AcademicRecord(student, "Operating Systems", 90, 5)
        ));
        studyPlanRepository.saveAll(List.of(
                new StudyPlan(student, "Databases", 20, 5),
                new StudyPlan(student, "Operating Systems", 20, 15)
        ));

        LocalDate today = LocalDate.now();
        taskRepository.saveAll(List.of(
                new Task(student, "Today medium", today, Task.Priority.MEDIUM),
                new Task(student, "Tomorrow low", today.plusDays(1), Task.Priority.LOW),
                new Task(student, "Tomorrow high", today.plusDays(1), Task.Priority.HIGH),
                new Task(student, "Later one", today.plusDays(2), Task.Priority.MEDIUM),
                new Task(student, "Later two", today.plusDays(3), Task.Priority.LOW),
                new Task(student, "Later three", today.plusDays(4), Task.Priority.HIGH)
        ));
        Task completed = new Task(student, "Completed task", today, Task.Priority.HIGH);
        completed.markCompleted();
        taskRepository.save(completed);

        Habit habit = new Habit(student, "Read", 4);
        habit.recordCompletion(today);
        habitRepository.save(habit);
        calendarEventRepository.saveAll(List.of(
                new CalendarEvent(student, "Later event", today.plusDays(4)),
                new CalendarEvent(student, "Soon event", today.plusDays(1))
        ));
        careerRepository.save(new CareerProfile(
                student, "Backend Developer", List.of("Java", "Databases")
        ));
        skillRepository.saveAll(List.of(
                new Skill(student, "Java", Skill.Level.INTERMEDIATE),
                new Skill(student, "SQL", Skill.Level.ADVANCED)
        ));

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academics.subjectCount").value(3))
                .andExpect(jsonPath("$.academics.averageScore").value(closeTo(66.666, 0.01)))
                .andExpect(jsonPath("$.academics.strongestSubject.subject").value("Operating Systems"))
                .andExpect(jsonPath("$.academics.attentionSubject.subject").value("Mathematics"))
                .andExpect(jsonPath("$.studyPlans.totalPlans").value(2))
                .andExpect(jsonPath("$.studyPlans.averageProgress").value(50.0))
                .andExpect(jsonPath("$.studyPlans.plans[0].progress").value(25.0))
                .andExpect(jsonPath("$.tasks.pendingTaskCount").value(6))
                .andExpect(jsonPath("$.tasks.highPriorityPendingCount").value(2))
                .andExpect(jsonPath("$.tasks.nextDueTasks.length()").value(5))
                .andExpect(jsonPath("$.tasks.nextDueTasks[0].title").value("Today medium"))
                .andExpect(jsonPath("$.tasks.nextDueTasks[1].title").value("Tomorrow high"))
                .andExpect(jsonPath("$.tasks.nextDueTasks[2].title").value("Tomorrow low"))
                .andExpect(jsonPath("$.habits.totalHabits").value(1))
                .andExpect(jsonPath("$.habits.habits[0].currentStreak").value(1))
                .andExpect(jsonPath("$.habits.habits[0].progress").value(25.0))
                .andExpect(jsonPath("$.calendar.upcomingEvents[0].title").value("Soon event"))
                .andExpect(jsonPath("$.calendar.upcomingEvents[1].title").value("Later event"))
                .andExpect(jsonPath("$.career.setupNeeded").value(false))
                .andExpect(jsonPath("$.career.careerGoal").value("Backend Developer"))
                .andExpect(jsonPath("$.career.interests.length()").value(2))
                .andExpect(jsonPath("$.career.skillCount").value(2))
                .andExpect(jsonPath("$.career.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.career.skills[1].name").value("SQL"));

        assertEquals(0, provider.calls);
    }

    @Test
    void calendarExcludesPastEventsAndOrdersUpcomingEvents() throws Exception {
        String email = "calendar-dashboard@example.com";
        String token = register(email, "Calendar Student", "CSE", 3);
        Student student = student(email);
        LocalDate today = LocalDate.now();
        CalendarEvent past = calendarEventRepository.saveAndFlush(
                new CalendarEvent(student, "Past event", today)
        );
        jdbcTemplate.update(
                "update calendar_events set event_date = ? where id = ?",
                Date.valueOf(today.minusDays(1)), past.getId()
        );
        calendarEventRepository.saveAll(List.of(
                new CalendarEvent(student, "Third", today.plusDays(3)),
                new CalendarEvent(student, "First", today.plusDays(1)),
                new CalendarEvent(student, "Second", today.plusDays(2))
        ));

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendar.upcomingEventCount").value(3))
                .andExpect(jsonPath("$.calendar.upcomingEvents.length()").value(3))
                .andExpect(jsonPath("$.calendar.upcomingEvents[0].title").value("First"))
                .andExpect(jsonPath("$.calendar.upcomingEvents[1].title").value("Second"))
                .andExpect(jsonPath("$.calendar.upcomingEvents[2].title").value("Third"));

        assertEquals(0, provider.calls);
    }

    @Test
    void recommendationPreviewContainsOnlyActiveItemsInPriorityOrderAndIsLimited() throws Exception {
        String email = "recommendation-dashboard@example.com";
        String token = register(email, "Recommendation Student", "CSE", 4);
        Student student = student(email);
        recommendationRepository.saveAll(List.of(
                recommendation(student, Recommendation.Priority.LOW, "Low item"),
                recommendation(student, Recommendation.Priority.MEDIUM, "Medium item"),
                recommendation(student, Recommendation.Priority.HIGH, "High item"),
                recommendation(student, Recommendation.Priority.URGENT, "Urgent item")
        ));
        Recommendation completed = recommendation(
                student, Recommendation.Priority.URGENT, "Completed urgent item"
        );
        completed.markCompleted();
        recommendationRepository.save(completed);

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.activeCount").value(4))
                .andExpect(jsonPath("$.recommendations.activeRecommendations.length()").value(3))
                .andExpect(jsonPath("$.recommendations.activeRecommendations[0].priority").value("URGENT"))
                .andExpect(jsonPath("$.recommendations.activeRecommendations[1].priority").value("HIGH"))
                .andExpect(jsonPath("$.recommendations.activeRecommendations[2].priority").value("MEDIUM"));

        assertEquals(0, provider.calls);
    }

    @Test
    void dashboardNeverIncludesAnotherStudentsFeatureData() throws Exception {
        String firstEmail = "dashboard-owner@example.com";
        String firstToken = register(firstEmail, "Owner", "CSE", 4);
        Student first = student(firstEmail);
        String secondEmail = "dashboard-other@example.com";
        register(secondEmail, "Other", "ECE", 6);
        Student second = student(secondEmail);

        academicRepository.save(new AcademicRecord(first, "Owner subject", 80, 4));
        academicRepository.save(new AcademicRecord(second, "Other subject", 20, 6));
        studyPlanRepository.save(new StudyPlan(second, "Other plan", 10, 5));
        taskRepository.save(new Task(second, "Other task", LocalDate.now(), Task.Priority.HIGH));
        habitRepository.save(new Habit(second, "Other habit", 5));
        calendarEventRepository.save(new CalendarEvent(
                second, "Other event", LocalDate.now().plusDays(1)
        ));
        careerRepository.save(new CareerProfile(second, "Other goal", List.of("Other interest")));
        skillRepository.save(new Skill(second, "Other skill", Skill.Level.ADVANCED));
        recommendationRepository.save(recommendation(
                second, Recommendation.Priority.URGENT, "Other recommendation"
        ));

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.name").value("Owner"))
                .andExpect(jsonPath("$.academics.subjectCount").value(1))
                .andExpect(jsonPath("$.academics.subjects[0].subject").value("Owner subject"))
                .andExpect(jsonPath("$.studyPlans.totalPlans").value(0))
                .andExpect(jsonPath("$.tasks.pendingTaskCount").value(0))
                .andExpect(jsonPath("$.habits.totalHabits").value(0))
                .andExpect(jsonPath("$.calendar.upcomingEventCount").value(0))
                .andExpect(jsonPath("$.career.setupNeeded").value(true))
                .andExpect(jsonPath("$.career.skillCount").value(0))
                .andExpect(jsonPath("$.recommendations.activeCount").value(0));

        assertEquals(0, provider.calls);
    }

    private Recommendation recommendation(
            Student student,
            Recommendation.Priority priority,
            String title
    ) {
        return new Recommendation(
                student,
                Recommendation.Category.PRODUCTIVITY,
                priority,
                title,
                "Grounded stored recommendation"
        );
    }

    private Student student(String email) {
        return studentRepository.findByUserEmail(email).orElseThrow();
    }

    private String register(String email, String name, String branch, int semester) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, "Password1!", name, branch, semester)
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderTestConfig {

        @Bean
        @Primary
        CountingRecommendationProvider countingRecommendationProvider() {
            return new CountingRecommendationProvider();
        }
    }

    static class CountingRecommendationProvider implements RecommendationProvider {

        private int calls;

        @Override
        public List<GeneratedRecommendation> generate(StudentContext context) {
            calls++;
            return List.of();
        }
    }

    private record Registration(
            String email,
            String password,
            String name,
            String branch,
            int semester
    ) {
    }
}
