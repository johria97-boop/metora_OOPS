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
import com.mentora.recommendation.RecommendationRepository;
import com.mentora.roadmap.Roadmap;
import com.mentora.roadmap.RoadmapProvider;
import com.mentora.roadmap.RoadmapRepository;
import com.mentora.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PhaseTenRoadmapIntegrationTests.ProviderTestConfig.class)
class PhaseTenRoadmapIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubRoadmapProvider provider;

    @Autowired
    private RoadmapRepository roadmapRepository;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        provider.reset();
        roadmapRepository.deleteAll();
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
    void authenticatedStudentGeneratesAndPersistsRoadmap() throws Exception {
        String token = register("roadmap-generate@example.com", "Roadmap Student");

        JsonNode response = generate(token, "Java Backend Development", "BEGINNER", 12);

        assertEquals("Java Backend Development", response.path("courseName").asText());
        assertEquals("BEGINNER", response.path("currentLevel").asText());
        assertEquals(3, response.path("steps").size());
        assertEquals(1, roadmapRepository.count());
        assertEquals(3, jdbcTemplate.queryForObject(
                "select count(*) from roadmap_steps", Integer.class
        ));
    }

    @Test
    void unauthenticatedGenerationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/roadmaps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new GenerateInput("Java", "BEGINNER", 8))))
                .andExpect(status().isUnauthorized());
        assertEquals(0, provider.calls);
    }

    @Test
    void blankCourseNameIsRejectedBeforeProviderCall() throws Exception {
        String token = register("roadmap-blank@example.com", "Blank Course");
        generateExpecting(token, "   ", "BEGINNER", 8, 400);
        assertEquals(0, provider.calls);
    }

    @Test
    void oversizedCourseNameIsRejectedBeforeProviderCall() throws Exception {
        String token = register("roadmap-long@example.com", "Long Course");
        generateExpecting(token, "x".repeat(121), "BEGINNER", 8, 400);
        assertEquals(0, provider.calls);
    }

    @Test
    void optionalIntermediateLevelIsAccepted() throws Exception {
        String token = register("roadmap-level@example.com", "Level Student");
        JsonNode response = generate(token, "Spring Boot", "INTERMEDIATE", null);

        assertEquals("INTERMEDIATE", response.path("currentLevel").asText());
        assertEquals(Roadmap.Level.INTERMEDIATE, provider.lastRequest.currentLevel());
    }

    @Test
    void absentLevelDefaultsToBeginner() throws Exception {
        String token = register("roadmap-default@example.com", "Default Level");
        JsonNode response = generate(token, "Cyber Security", null, null);

        assertEquals("BEGINNER", response.path("currentLevel").asText());
        assertEquals(Roadmap.Level.BEGINNER, provider.lastRequest.currentLevel());
    }

    @Test
    void targetWeeksBelowRangeIsRejected() throws Exception {
        String token = register("roadmap-week-low@example.com", "Low Weeks");
        generateExpecting(token, "Java", "BEGINNER", 0, 400);
        assertEquals(0, provider.calls);
    }

    @Test
    void targetWeeksAboveRangeIsRejected() throws Exception {
        String token = register("roadmap-week-high@example.com", "High Weeks");
        generateExpecting(token, "Java", "BEGINNER", 53, 400);
        assertEquals(0, provider.calls);
    }

    @Test
    void providerReceivesNormalizedCourseDataAndOptions() throws Exception {
        String token = register("roadmap-provider@example.com", "Provider Data");
        generate(token, "  Java   Backend Development  ", "ADVANCED", 16);

        assertNotNull(provider.lastRequest);
        assertEquals("Java Backend Development", provider.lastRequest.courseName());
        assertEquals(Roadmap.Level.ADVANCED, provider.lastRequest.currentLevel());
        assertEquals(16, provider.lastRequest.targetWeeks());
    }

    @Test
    void generatedRoadmapSurvivesRepositoryReload() throws Exception {
        String token = register("roadmap-reload@example.com", "Reload Student");
        long id = generate(token, "Machine Learning", "BEGINNER", 12).path("id").asLong();

        mockMvc.perform(get("/api/roadmaps/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseName").value("Machine Learning"))
                .andExpect(jsonPath("$.steps.length()").value(3))
                .andExpect(jsonPath("$.normalizedCourseName").doesNotExist())
                .andExpect(jsonPath("$.student").doesNotExist());
    }

    @Test
    void generatedStepsAreSortedAndRenumberedDeterministically() throws Exception {
        String token = register("roadmap-order@example.com", "Ordering Student");
        provider.output(new RoadmapProvider.GeneratedRoadmap("Ordered roadmap", List.of(
                step(2, "Advanced", 9, "Third", 5),
                step(1, "Foundations", 3, "First", 4),
                step(1, "Foundations", 3, "Second", 4)
        )));

        JsonNode response = generate(token, "Ordering", "BEGINNER", 6);

        assertEquals("First", response.path("steps").get(0).path("title").asText());
        assertEquals("Second", response.path("steps").get(1).path("title").asText());
        assertEquals("Third", response.path("steps").get(2).path("title").asText());
        assertEquals(1, response.path("steps").get(0).path("position").asInt());
        assertEquals(3, response.path("steps").get(2).path("position").asInt());
        assertEquals(2, response.path("steps").get(2).path("phaseNumber").asInt());
    }

    @Test
    void nullProviderRoadmapIsRejectedWithoutPersistence() throws Exception {
        String token = register("roadmap-null@example.com", "Null Provider");
        provider.output(null);

        generateExpecting(token, "Null Output", "BEGINNER", 8, 502);
        assertEquals(0, roadmapRepository.count());
    }

    @Test
    void tooFewGeneratedStepsAreRejected() throws Exception {
        String token = register("roadmap-few@example.com", "Few Steps");
        provider.output(new RoadmapProvider.GeneratedRoadmap("Too short", List.of(
                step(1, "Phase", 1, "One", 2), step(1, "Phase", 2, "Two", 2)
        )));

        generateExpecting(token, "Too Few", "BEGINNER", 8, 502);
    }

    @Test
    void moreThanTwentyGeneratedStepsAreRejected() throws Exception {
        String token = register("roadmap-many@example.com", "Many Steps");
        List<RoadmapProvider.GeneratedStep> steps = new ArrayList<>();
        for (int index = 1; index <= 21; index++) {
            steps.add(step(1, "Phase", index, "Step " + index, 2));
        }
        provider.output(new RoadmapProvider.GeneratedRoadmap("Too many", steps));

        generateExpecting(token, "Too Many", "BEGINNER", 8, 502);
        assertEquals(0, roadmapRepository.count());
    }

    @Test
    void blankGeneratedSummaryIsRejected() throws Exception {
        String token = register("roadmap-summary@example.com", "Blank Summary");
        provider.output(new RoadmapProvider.GeneratedRoadmap(" ", validSteps()));
        generateExpecting(token, "Blank Summary", "BEGINNER", 8, 502);
    }

    @Test
    void blankGeneratedStepTitleIsRejected() throws Exception {
        String token = register("roadmap-step-blank@example.com", "Blank Step");
        provider.output(new RoadmapProvider.GeneratedRoadmap("Summary", List.of(
                step(1, "Phase", 1, " ", 2),
                step(1, "Phase", 2, "Two", 2),
                step(1, "Phase", 3, "Three", 2)
        )));
        generateExpecting(token, "Blank Step", "BEGINNER", 8, 502);
    }

    @Test
    void invalidGeneratedEstimatedHoursAreRejected() throws Exception {
        String token = register("roadmap-hours@example.com", "Invalid Hours");
        provider.output(new RoadmapProvider.GeneratedRoadmap("Summary", List.of(
                step(1, "Phase", 1, "One", 0),
                step(1, "Phase", 2, "Two", 2),
                step(1, "Phase", 3, "Three", 2)
        )));
        generateExpecting(token, "Invalid Hours", "BEGINNER", 8, 502);
    }

    @Test
    void conflictingTitlesForOneGeneratedPhaseAreRejected() throws Exception {
        String token = register("roadmap-phase@example.com", "Invalid Phase");
        provider.output(new RoadmapProvider.GeneratedRoadmap("Summary", List.of(
                step(1, "Foundations", 1, "One", 2),
                step(1, "Different", 2, "Two", 2),
                step(2, "Advanced", 3, "Three", 2)
        )));
        generateExpecting(token, "Invalid Phase", "BEGINNER", 8, 502);
    }

    @Test
    void providerFailureReturnsControlledBadGateway() throws Exception {
        String token = register("roadmap-failure@example.com", "Provider Failure");
        provider.fail = true;

        mockMvc.perform(post("/api/roadmaps/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new GenerateInput("Java", "BEGINNER", 8))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Roadmap provider is temporarily unavailable"));
    }

    @Test
    void duplicateActiveCourseReturnsConflictWithoutProviderCall() throws Exception {
        String token = register("roadmap-duplicate@example.com", "Duplicate Course");
        generate(token, "React", "BEGINNER", 8);

        generateExpecting(token, "React", "ADVANCED", 4, 409);
        assertEquals(1, provider.calls);
    }

    @Test
    void duplicateCourseNormalizationIgnoresCaseAndRepeatedWhitespace() throws Exception {
        String token = register("roadmap-normalized@example.com", "Normalized Course");
        generate(token, "Data Structures and Algorithms", "BEGINNER", 8);

        generateExpecting(token, "  data   structures AND algorithms ", "BEGINNER", 8, 409);
        assertEquals(1, provider.calls);
    }

    @Test
    void completedCourseMayBeGeneratedAgain() throws Exception {
        String token = register("roadmap-regenerate@example.com", "Regenerate Course");
        JsonNode first = generate(token, "Cloud Computing", "BEGINNER", 8);
        completeEveryStep(token, first);

        JsonNode second = generate(token, "cloud computing", "INTERMEDIATE", 6);

        assertEquals(2, roadmapRepository.count());
        assertEquals(2, provider.calls);
        assertEquals("cloud computing", second.path("courseName").asText());
    }

    @Test
    void completingOneStepUpdatesProgressAndPersists() throws Exception {
        String token = register("roadmap-progress@example.com", "Progress Student");
        JsonNode roadmap = generate(token, "Python", "BEGINNER", 8);
        long roadmapId = roadmap.path("id").asLong();
        long stepId = roadmap.path("steps").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/roadmaps/{roadmapId}/steps/{stepId}/status", roadmapId, stepId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusInput("COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSteps").value(1))
                .andExpect(jsonPath("$.progress").value(100.0 / 3.0));

        mockMvc.perform(get("/api/roadmaps/{id}", roadmapId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.completedSteps").value(1))
                .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"));
    }

    @Test
    void inProgressStepDoesNotCountAsCompleted() throws Exception {
        String token = register("roadmap-in-progress@example.com", "In Progress");
        JsonNode roadmap = generate(token, "IoT", "BEGINNER", 8);
        long roadmapId = roadmap.path("id").asLong();
        long stepId = roadmap.path("steps").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/roadmaps/{roadmapId}/steps/{stepId}/status", roadmapId, stepId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusInput("IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(0.0))
                .andExpect(jsonPath("$.completedSteps").value(0));
    }

    @Test
    void allCompletedStepsProduceCompletedRoadmapAtOneHundredPercent() throws Exception {
        String token = register("roadmap-complete@example.com", "Complete Roadmap");
        JsonNode roadmap = generate(token, "Web Development", "BEGINNER", 8);

        JsonNode completed = completeEveryStep(token, roadmap);

        assertEquals(100.0, completed.path("progress").asDouble());
        assertEquals("COMPLETED", completed.path("state").asText());
    }

    @Test
    void anotherStudentCannotReadRoadmapByGuessedId() throws Exception {
        String owner = register("roadmap-owner-read@example.com", "Owner");
        String attacker = register("roadmap-attacker-read@example.com", "Attacker");
        long id = generate(owner, "Java", "BEGINNER", 8).path("id").asLong();

        mockMvc.perform(get("/api/roadmaps/{id}", id).header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherStudentCannotUpdateRoadmapStep() throws Exception {
        String owner = register("roadmap-owner-update@example.com", "Owner");
        String attacker = register("roadmap-attacker-update@example.com", "Attacker");
        JsonNode roadmap = generate(owner, "Java", "BEGINNER", 8);

        mockMvc.perform(patch("/api/roadmaps/{roadmapId}/steps/{stepId}/status",
                        roadmap.path("id").asLong(), roadmap.path("steps").get(0).path("id").asLong())
                        .header("Authorization", bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusInput("COMPLETED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherStudentCannotDeleteRoadmap() throws Exception {
        String owner = register("roadmap-owner-delete@example.com", "Owner");
        String attacker = register("roadmap-attacker-delete@example.com", "Attacker");
        long id = generate(owner, "Java", "BEGINNER", 8).path("id").asLong();

        mockMvc.perform(delete("/api/roadmaps/{id}", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
        assertEquals(1, roadmapRepository.count());
    }

    @Test
    void stepFromAnotherOwnedRoadmapCannotBeUpdatedThroughValidRoadmapId() throws Exception {
        String token = register("roadmap-cross-step@example.com", "Cross Step");
        JsonNode first = generate(token, "Java", "BEGINNER", 8);
        JsonNode second = generate(token, "React", "BEGINNER", 8);

        mockMvc.perform(patch("/api/roadmaps/{roadmapId}/steps/{stepId}/status",
                        first.path("id").asLong(), second.path("steps").get(0).path("id").asLong())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusInput("COMPLETED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingOwnedRoadmapCascadesItsComposedSteps() throws Exception {
        String token = register("roadmap-cascade@example.com", "Cascade Student");
        long id = generate(token, "Java", "BEGINNER", 8).path("id").asLong();

        mockMvc.perform(delete("/api/roadmaps/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertEquals(0, roadmapRepository.count());
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from roadmap_steps where roadmap_id = ?", Integer.class, id
        ));
    }

    @Test
    void invalidStepStatusReturnsBadRequest() throws Exception {
        String token = register("roadmap-status@example.com", "Invalid Status");
        JsonNode roadmap = generate(token, "Java", "BEGINNER", 8);

        mockMvc.perform(patch("/api/roadmaps/{roadmapId}/steps/{stepId}/status",
                        roadmap.path("id").asLong(), roadmap.path("steps").get(0).path("id").asLong())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listingStoredRoadmapsNeverCallsProvider() throws Exception {
        String token = register("roadmap-list@example.com", "List Student");
        generate(token, "Java", "BEGINNER", 8);
        provider.calls = 0;

        mockMvc.perform(get("/api/roadmaps").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertEquals(0, provider.calls);
    }

    @Test
    void dashboardShowsStoredRoadmapSummaryWithoutCallingProvider() throws Exception {
        String token = register("roadmap-dashboard@example.com", "Dashboard Student");
        JsonNode roadmap = generate(token, "Java", "BEGINNER", 8);
        updateStatus(token, roadmap, 0, "COMPLETED");
        provider.calls = 0;

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmaps.totalRoadmaps").value(1))
                .andExpect(jsonPath("$.roadmaps.activeRoadmapCount").value(1))
                .andExpect(jsonPath("$.roadmaps.currentRoadmap.courseName").value("Java"))
                .andExpect(jsonPath("$.roadmaps.currentRoadmap.completedSteps").value(1));
        assertEquals(0, provider.calls);
    }

    @Test
    void dashboardNeverShowsAnotherStudentsRoadmap() throws Exception {
        String first = register("roadmap-dashboard-owner@example.com", "Dashboard Owner");
        String second = register("roadmap-dashboard-other@example.com", "Dashboard Other");
        generate(second, "Private Course", "BEGINNER", 8);
        provider.calls = 0;

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadmaps.totalRoadmaps").value(0))
                .andExpect(jsonPath("$.roadmaps.activeRoadmapCount").value(0))
                .andExpect(jsonPath("$.roadmaps.currentRoadmap").doesNotExist());
        assertEquals(0, provider.calls);
    }

    private JsonNode completeEveryStep(String token, JsonNode roadmap) throws Exception {
        JsonNode current = roadmap;
        for (JsonNode step : roadmap.path("steps")) {
            current = updateStatus(token, roadmap, step.path("id").asLong(), "COMPLETED");
        }
        return current;
    }

    private JsonNode updateStatus(
            String token,
            JsonNode roadmap,
            int stepIndex,
            String statusValue
    ) throws Exception {
        return updateStatus(
                token, roadmap, roadmap.path("steps").get(stepIndex).path("id").asLong(), statusValue
        );
    }

    private JsonNode updateStatus(
            String token,
            JsonNode roadmap,
            long stepId,
            String statusValue
    ) throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/roadmaps/{roadmapId}/steps/{stepId}/status",
                        roadmap.path("id").asLong(), stepId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusInput(statusValue))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode generate(
            String token,
            String courseName,
            String level,
            Integer targetWeeks
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/roadmaps/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new GenerateInput(courseName, level, targetWeeks))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void generateExpecting(
            String token,
            String courseName,
            String level,
            Integer targetWeeks,
            int expectedStatus
    ) throws Exception {
        mockMvc.perform(post("/api/roadmaps/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new GenerateInput(courseName, level, targetWeeks))))
                .andExpect(status().is(expectedStatus));
    }

    private String register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new Registration(email, "Password1!", name, "CSE", 3))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private List<RoadmapProvider.GeneratedStep> validSteps() {
        return List.of(
                step(1, "Foundations", 1, "Learn foundations", 4),
                step(1, "Foundations", 2, "Practice basics", 5),
                step(2, "Application", 3, "Build a project", 8)
        );
    }

    private RoadmapProvider.GeneratedStep step(
            int phaseNumber,
            String phaseTitle,
            int position,
            String title,
            int hours
    ) {
        return new RoadmapProvider.GeneratedStep(
                phaseNumber, phaseTitle, position, title,
                "A concise and actionable learning step.", hours
        );
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
        StubRoadmapProvider stubRoadmapProvider() {
            return new StubRoadmapProvider();
        }
    }

    static class StubRoadmapProvider implements RoadmapProvider {

        private GeneratedRoadmap generated;
        private RoadmapRequest lastRequest;
        private int calls;
        private boolean fail;

        StubRoadmapProvider() {
            reset();
        }

        @Override
        public GeneratedRoadmap generate(RoadmapRequest request) {
            calls++;
            lastRequest = request;
            if (fail) {
                throw new IllegalStateException("Simulated roadmap provider failure");
            }
            return generated;
        }

        void output(GeneratedRoadmap generated) {
            this.generated = generated;
        }

        void reset() {
            generated = new GeneratedRoadmap("A practical learning roadmap.", List.of(
                    new GeneratedStep(1, "Foundations", 1, "Learn foundations",
                            "Understand the essential concepts.", 4),
                    new GeneratedStep(1, "Foundations", 2, "Practice basics",
                            "Apply the foundational concepts.", 5),
                    new GeneratedStep(2, "Application", 3, "Build a project",
                            "Combine the concepts in a practical project.", 8)
            ));
            lastRequest = null;
            calls = 0;
            fail = false;
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

    private record GenerateInput(String courseName, String currentLevel, Integer targetWeeks) {
    }

    private record StatusInput(String status) {
    }
}
