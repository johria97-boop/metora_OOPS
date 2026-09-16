package com.mentora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.auth.UserRepository;
import com.mentora.career.CareerRepository;
import com.mentora.career.SkillRepository;
import com.mentora.productivity.CalendarEventRepository;
import com.mentora.productivity.HabitRepository;
import com.mentora.productivity.TaskRepository;
import com.mentora.recommendation.RecommendationProvider;
import com.mentora.recommendation.RecommendationRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PhaseSixIntegrationTests.ProviderTestConfig.class)
class PhaseSixIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubRecommendationProvider provider;

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
        provider.reset();
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
    void unauthenticatedRecommendationRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/recommendations/generate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void insufficientContextReturnsControlledBadRequestWithoutCallingProvider() throws Exception {
        String token = register("empty-context@example.com", "Empty Context");

        mockMvc.perform(post("/api/recommendations/generate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Add academic, productivity, or career information before generating recommendations"
                ));
        assertEquals(0, provider.calls);
    }

    @Test
    void generationUsesCompleteSafeStudentContextAndPersistsActiveOutput() throws Exception {
        String token = register("context@example.com", "Context Student");
        addAllContext(token);
        provider.output(List.of(generated(
                "ACADEMIC", "HIGH", "Strengthen DSA", "Your DSA score is 42%."
        )));

        mockMvc.perform(post("/api/recommendations/generate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("ACADEMIC"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].normalizedTitle").doesNotExist())
                .andExpect(jsonPath("$[0].student").doesNotExist());

        RecommendationProvider.StudentContext context = provider.lastContext;
        assertNotNull(context);
        assertEquals("CSE", context.branch());
        assertEquals(3, context.semester());
        assertEquals(1, context.academicRecords().size());
        assertEquals(1, context.studyPlans().size());
        assertEquals(1, context.pendingTasks().size());
        assertEquals(1, context.habits().size());
        assertEquals(1, context.calendarEvents().size());
        assertEquals("Backend Developer", context.career().careerGoal());
        assertEquals(List.of("Software Development"), context.career().interests());
        assertEquals(1, context.skills().size());

        String serialized = objectMapper.writeValueAsString(context).toLowerCase();
        assertFalse(serialized.contains("\"email\""));
        assertFalse(serialized.contains("password"));
        assertFalse(serialized.contains("jwt"));
        assertFalse(serialized.contains("normalizedname"));
        assertFalse(serialized.contains("studentid"));
        assertFalse(serialized.contains("userid"));
    }

    @Test
    void invalidProviderEntriesAreIgnoredAndMaximumFiveAreStored() throws Exception {
        String token = register("validation@example.com", "Validation Student");
        addAcademicRecord(token);
        List<RecommendationProvider.GeneratedRecommendation> output = new ArrayList<>(List.of(
                generated("UNKNOWN", "HIGH", "Invalid category", "Reason"),
                generated("ACADEMIC", "UNKNOWN", "Invalid priority", "Reason"),
                generated("ACADEMIC", "HIGH", "  ", "Reason"),
                generated("ACADEMIC", "HIGH", "Blank reason", "  ")
        ));
        for (int index = 1; index <= 6; index++) {
            output.add(generated("ACADEMIC", "MEDIUM", "Valid " + index, "Grounded reason " + index));
        }
        provider.output(output);

        mockMvc.perform(post("/api/recommendations/generate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        assertEquals(5, recommendationRepository.count());
    }

    @Test
    void activeRecommendationsDeduplicateCaseAndRepeatedWhitespace() throws Exception {
        String token = register("dedupe@example.com", "Dedupe Student");
        addAcademicRecord(token);
        provider.output(List.of(generated(
                "ACADEMIC", "HIGH", "Improve DSA Fundamentals", "Current score needs attention."
        )));
        long firstId = generateAndGetFirstId(token);

        provider.output(List.of(generated(
                "academic", "LOW", "  improve   dsa fundamentals ", "A different provider reason."
        )));
        long secondId = generateAndGetFirstId(token);

        assertEquals(firstId, secondId);
        assertEquals(1, recommendationRepository.count());
    }

    @Test
    void providerFailureIsControlledAndStoredRecommendationsRemainRetrievable() throws Exception {
        String token = register("provider-failure@example.com", "Provider Failure");
        addAcademicRecord(token);
        provider.output(List.of(generated(
                "ACADEMIC", "MEDIUM", "Review DSA", "Your stored score provides context."
        )));
        generateAndGetFirstId(token);
        provider.fail = true;

        mockMvc.perform(post("/api/recommendations/generate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(
                        "Recommendation provider is temporarily unavailable"
                ));
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Review DSA"));
    }

    @Test
    void completesOwnedRecommendationAndKeepsItInHistory() throws Exception {
        String token = register("complete-rec@example.com", "Complete Recommendation");
        long id = seedRecommendation(token, "Complete this plan");

        mockMvc.perform(patch("/api/recommendations/{id}/complete", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(get("/api/recommendations").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void dismissesOwnedRecommendationAndKeepsItInHistory() throws Exception {
        String token = register("dismiss-rec@example.com", "Dismiss Recommendation");
        long id = seedRecommendation(token, "Optional next step");

        mockMvc.perform(patch("/api/recommendations/{id}/dismiss", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
        mockMvc.perform(get("/api/recommendations").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DISMISSED"));
    }

    @Test
    void anotherStudentCannotReadCompleteOrDismissRecommendation() throws Exception {
        String owner = register("rec-owner@example.com", "Recommendation Owner");
        String attacker = register("rec-attacker@example.com", "Recommendation Attacker");
        long id = seedRecommendation(owner, "Private recommendation");

        mockMvc.perform(get("/api/recommendations").header("Authorization", bearer(attacker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(patch("/api/recommendations/{id}/complete", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/recommendations/{id}/dismiss", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
    }

    private void addAllContext(String token) throws Exception {
        addAcademicRecord(token);
        mockMvc.perform(post("/api/academic/study-plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StudyPlanInput("DSA", 20, 5))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/productivity/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TaskInput(
                                "Submit assignment", LocalDate.now().plusDays(1), "HIGH"
                        ))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/productivity/habits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new HabitInput("Practice coding", 10))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/productivity/calendar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CalendarInput("Project review", LocalDate.now().plusDays(3)))))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CareerInput(
                                "Backend Developer", List.of("Software Development")
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SkillInput("Java", "INTERMEDIATE"))))
                .andExpect(status().isCreated());
    }

    private void addAcademicRecord(String token) throws Exception {
        mockMvc.perform(post("/api/academic/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AcademicInput("DSA", 42, 3))))
                .andExpect(status().isCreated());
    }

    private long seedRecommendation(String token, String title) throws Exception {
        addAcademicRecord(token);
        provider.output(List.of(generated(
                "ACADEMIC", "MEDIUM", title, "Grounded in the stored DSA score."
        )));
        return generateAndGetFirstId(token);
    }

    private long generateAndGetFirstId(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recommendations/generate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    private String register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new Registration(email, "Password1!", name, "CSE", 3))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private RecommendationProvider.GeneratedRecommendation generated(
            String category,
            String priority,
            String title,
            String reason
    ) {
        return new RecommendationProvider.GeneratedRecommendation(category, priority, title, reason);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderTestConfig {

        @Bean
        @Primary
        StubRecommendationProvider stubRecommendationProvider() {
            return new StubRecommendationProvider();
        }
    }

    static class StubRecommendationProvider implements RecommendationProvider {

        private List<GeneratedRecommendation> recommendations = List.of();
        private StudentContext lastContext;
        private int calls;
        private boolean fail;

        @Override
        public List<GeneratedRecommendation> generate(StudentContext context) {
            calls++;
            lastContext = context;
            if (fail) {
                throw new IllegalStateException("Simulated provider failure");
            }
            return recommendations;
        }

        void output(List<GeneratedRecommendation> recommendations) {
            this.recommendations = recommendations;
        }

        void reset() {
            recommendations = List.of();
            lastContext = null;
            calls = 0;
            fail = false;
        }
    }

    private record Registration(String email, String password, String name, String branch, int semester) {
    }

    private record AcademicInput(String subject, double score, int semester) {
    }

    private record StudyPlanInput(String subject, double targetHours, double completedHours) {
    }

    private record TaskInput(String title, LocalDate dueDate, String priority) {
    }

    private record HabitInput(String name, int targetStreak) {
    }

    private record CalendarInput(String title, LocalDate eventDate) {
    }

    private record CareerInput(String careerGoal, List<String> interests) {
    }

    private record SkillInput(String name, String level) {
    }
}
