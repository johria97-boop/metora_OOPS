package com.mentora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.auth.UserRepository;
import com.mentora.career.CareerRepository;
import com.mentora.career.SkillRepository;
import com.mentora.student.StudentRepository;
import com.mentora.productivity.CalendarEventRepository;
import com.mentora.productivity.HabitRepository;
import com.mentora.productivity.TaskRepository;
import com.mentora.recommendation.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class PhaseTwoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicRepository academicRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @BeforeEach
    void clearDatabase() {
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
    void createsValidAcademicRecord() throws Exception {
        String token = register("record@example.com", "Record Student");

        mockMvc.perform(post("/api/academic/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Mathematics", 88.5, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.subject").value("Mathematics"))
                .andExpect(jsonPath("$.score").value(88.5))
                .andExpect(jsonPath("$.semester").value(3))
                .andExpect(jsonPath("$.student").doesNotExist());
    }

    @Test
    void rejectsScoreBelowZero() throws Exception {
        String token = register("low-score@example.com", "Low Score");

        mockMvc.perform(post("/api/academic/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Mathematics", -0.1, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.score").exists());
    }

    @Test
    void rejectsScoreAboveOneHundred() throws Exception {
        String token = register("high-score@example.com", "High Score");

        mockMvc.perform(post("/api/academic/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Mathematics", 100.1, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.score").exists());
    }

    @Test
    void rejectsBlankAcademicSubject() throws Exception {
        String token = register("blank-record@example.com", "Blank Record");

        mockMvc.perform(post("/api/academic/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("   ", 75, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.subject").exists());
    }

    @Test
    void listsOnlyAuthenticatedStudentsAcademicRecords() throws Exception {
        String firstToken = register("first-records@example.com", "First Student");
        String secondToken = register("second-records@example.com", "Second Student");
        createRecord(firstToken, "Algorithms", 91, 4);
        createRecord(secondToken, "Networks", 82, 4);

        mockMvc.perform(get("/api/academic/records").header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subject").value("Algorithms"));
    }

    @Test
    void updatesOwnAcademicRecord() throws Exception {
        String token = register("update-record@example.com", "Update Student");
        long id = createRecord(token, "Math", 70, 2);

        mockMvc.perform(put("/api/academic/records/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Advanced Math", 89, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Advanced Math"))
                .andExpect(jsonPath("$.score").value(89.0))
                .andExpect(jsonPath("$.semester").value(3));
    }

    @Test
    void deletesOwnAcademicRecord() throws Exception {
        String token = register("delete-record@example.com", "Delete Student");
        long id = createRecord(token, "Physics", 80, 2);

        mockMvc.perform(delete("/api/academic/records/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/academic/records").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotUpdateAnotherStudentsAcademicRecord() throws Exception {
        String ownerToken = register("record-owner@example.com", "Record Owner");
        String attackerToken = register("record-attacker@example.com", "Record Attacker");
        long id = createRecord(ownerToken, "Chemistry", 77, 3);

        mockMvc.perform(put("/api/academic/records/{id}", id)
                        .header("Authorization", bearer(attackerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Tampered", 1, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Academic record not found"));

        mockMvc.perform(get("/api/academic/records").header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$[0].subject").value("Chemistry"))
                .andExpect(jsonPath("$[0].score").value(77.0));
    }

    @Test
    void cannotDeleteAnotherStudentsAcademicRecord() throws Exception {
        String ownerToken = register("delete-owner@example.com", "Delete Owner");
        String attackerToken = register("delete-attacker@example.com", "Delete Attacker");
        long id = createRecord(ownerToken, "Biology", 79, 3);

        mockMvc.perform(delete("/api/academic/records/{id}", id)
                        .header("Authorization", bearer(attackerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/academic/records").header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createsValidStudyPlan() throws Exception {
        String token = register("plan@example.com", "Plan Student");

        mockMvc.perform(post("/api/academic/study-plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Algorithms", 20, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Algorithms"))
                .andExpect(jsonPath("$.targetHours").value(20.0))
                .andExpect(jsonPath("$.completedHours").value(5.0))
                .andExpect(jsonPath("$.student").doesNotExist());
    }

    @Test
    void rejectsNonPositiveTargetHours() throws Exception {
        String token = register("zero-target@example.com", "Zero Target");

        mockMvc.perform(post("/api/academic/study-plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Algorithms", 0, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.targetHours").exists());
    }

    @Test
    void rejectsNegativeCompletedHours() throws Exception {
        String token = register("negative-progress@example.com", "Negative Progress");

        mockMvc.perform(post("/api/academic/study-plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Algorithms", 10, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.completedHours").exists());
    }

    @Test
    void rejectsCompletedHoursAboveTarget() throws Exception {
        String token = register("excess-progress@example.com", "Excess Progress");

        mockMvc.perform(post("/api/academic/study-plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Algorithms", 10, 11)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completed hours cannot exceed target hours"));
    }

    @Test
    void calculatesProgressAsBoundedPercentage() throws Exception {
        String token = register("progress@example.com", "Progress Student");
        long id = createStudyPlan(token, "Operating Systems", 40, 10);

        mockMvc.perform(get("/api/academic/study-plans").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].progress").value(25.0));
    }

    @Test
    void updatesStudyPlanProgress() throws Exception {
        String token = register("update-progress@example.com", "Progress Updater");
        long id = createStudyPlan(token, "Databases", 20, 2);

        mockMvc.perform(patch("/api/academic/study-plans/{id}/progress", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressJson(15)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedHours").value(15.0))
                .andExpect(jsonPath("$.progress").value(75.0));
    }

    @Test
    void listsOnlyAuthenticatedStudentsStudyPlans() throws Exception {
        String firstToken = register("first-plans@example.com", "First Planner");
        String secondToken = register("second-plans@example.com", "Second Planner");
        createStudyPlan(firstToken, "Databases", 12, 3);
        createStudyPlan(secondToken, "Networks", 10, 4);

        mockMvc.perform(get("/api/academic/study-plans").header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subject").value("Databases"));
    }

    @Test
    void updatesOwnStudyPlan() throws Exception {
        String token = register("edit-plan@example.com", "Plan Editor");
        long id = createStudyPlan(token, "Math", 10, 2);

        mockMvc.perform(put("/api/academic/study-plans/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Advanced Math", 16, 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Advanced Math"))
                .andExpect(jsonPath("$.targetHours").value(16.0))
                .andExpect(jsonPath("$.progress").value(25.0));
    }

    @Test
    void deletesOwnStudyPlan() throws Exception {
        String token = register("delete-plan@example.com", "Plan Deleter");
        long id = createStudyPlan(token, "Physics", 10, 1);

        mockMvc.perform(delete("/api/academic/study-plans/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/academic/study-plans").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotUpdateAnotherStudentsStudyPlan() throws Exception {
        String ownerToken = register("plan-owner@example.com", "Plan Owner");
        String attackerToken = register("plan-attacker@example.com", "Plan Attacker");
        long id = createStudyPlan(ownerToken, "Security", 15, 5);

        mockMvc.perform(put("/api/academic/study-plans/{id}", id)
                        .header("Authorization", bearer(attackerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Tampered", 1, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Study plan not found"));

        mockMvc.perform(get("/api/academic/study-plans").header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$[0].subject").value("Security"))
                .andExpect(jsonPath("$[0].targetHours").value(15.0));
    }

    @Test
    void cannotDeleteAnotherStudentsStudyPlan() throws Exception {
        String ownerToken = register("plan-delete-owner@example.com", "Plan Delete Owner");
        String attackerToken = register("plan-delete-attacker@example.com", "Plan Delete Attacker");
        long id = createStudyPlan(ownerToken, "Security", 15, 5);

        mockMvc.perform(delete("/api/academic/study-plans/{id}", id)
                        .header("Authorization", bearer(attackerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/academic/study-plans").header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unauthenticatedAcademicRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/academic/records"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/academic/study-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson("Algorithms", 10, 0)))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email, String name) throws Exception {
        String body = objectMapper.writeValueAsString(
                new Registration(email, "Password1!", name, "CSE", 3)
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createRecord(String token, String subject, double score, int semester) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/academic/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson(subject, score, semester)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private long createStudyPlan(
            String token,
            String subject,
            double targetHours,
            double completedHours
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/academic/study-plans")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson(subject, targetHours, completedHours)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String recordJson(String subject, double score, int semester) throws Exception {
        return objectMapper.writeValueAsString(new RecordInput(subject, score, semester));
    }

    private String planJson(String subject, double targetHours, double completedHours) throws Exception {
        return objectMapper.writeValueAsString(new PlanInput(subject, targetHours, completedHours));
    }

    private String progressJson(double completedHours) throws Exception {
        return objectMapper.writeValueAsString(new ProgressInput(completedHours));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String email, String password, String name, String branch, int semester) {
    }

    private record RecordInput(String subject, double score, int semester) {
    }

    private record PlanInput(String subject, double targetHours, double completedHours) {
    }

    private record ProgressInput(double completedHours) {
    }
}
