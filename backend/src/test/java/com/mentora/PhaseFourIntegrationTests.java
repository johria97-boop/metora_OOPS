package com.mentora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.auth.UserRepository;
import com.mentora.career.CareerRepository;
import com.mentora.career.Skill;
import com.mentora.career.SkillRepository;
import com.mentora.productivity.CalendarEventRepository;
import com.mentora.productivity.HabitRepository;
import com.mentora.productivity.TaskRepository;
import com.mentora.recommendation.RecommendationRepository;
import com.mentora.student.Student;
import com.mentora.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseFourIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private AcademicRepository academicRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

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
    void unauthenticatedCareerRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/career"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/career/skills"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCareerProfileReturnsSetupFriendlyEmptyResponse() throws Exception {
        String token = register("empty-career@example.com", "Empty Career");

        mockMvc.perform(get("/api/career").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerGoal").value(""))
                .andExpect(jsonPath("$.interests.length()").value(0));
    }

    @Test
    void upsertsRetrievesAndUpdatesSingleCareerProfile() throws Exception {
        String token = register("career@example.com", "Career Student");

        MvcResult created = mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerJson("Backend Developer", List.of("Java", "Databases"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.careerGoal").value("Backend Developer"))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/career").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.interests.length()").value(2));
        mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerJson("Cloud Engineer", List.of("Cloud"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.careerGoal").value("Cloud Engineer"));

        assertEquals(1, careerRepository.count());
    }

    @Test
    void rejectsBlankCareerGoalAndBlankInterest() throws Exception {
        String token = register("invalid-career@example.com", "Invalid Career");

        mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerJson("   ", List.of("AI"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.careerGoal").exists());
        mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerJson("Data Analyst", List.of("   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trimsAndDeduplicatesInterestsCaseInsensitively() throws Exception {
        String token = register("interests@example.com", "Interest Student");

        mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerJson(" AI Engineer ",
                                List.of("AI", " ai ", "Cyber Security", "AI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerGoal").value("AI Engineer"))
                .andExpect(jsonPath("$.interests.length()").value(2))
                .andExpect(jsonPath("$.interests[0]").value("AI"))
                .andExpect(jsonPath("$.interests[1]").value("Cyber Security"));
    }

    @Test
    void studentsReceiveOnlyTheirOwnCareerProfile() throws Exception {
        String first = register("first-career@example.com", "First Career");
        String second = register("second-career@example.com", "Second Career");
        upsertCareer(first, "Backend Developer");
        upsertCareer(second, "Data Analyst");

        mockMvc.perform(get("/api/career").header("Authorization", bearer(first)))
                .andExpect(jsonPath("$.careerGoal").value("Backend Developer"));
        mockMvc.perform(get("/api/career").header("Authorization", bearer(second)))
                .andExpect(jsonPath("$.careerGoal").value("Data Analyst"));
    }

    @Test
    void addsSkillWithDisplayNameAndLevelAndListsOnlyOwnSkills() throws Exception {
        String owner = register("skill-owner@example.com", "Skill Owner");
        String other = register("skill-other@example.com", "Skill Other");
        addSkill(owner, " Java ", "INTERMEDIATE");
        addSkill(other, "Python", "BEGINNER");

        mockMvc.perform(get("/api/career/skills").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].level").value("INTERMEDIATE"))
                .andExpect(jsonPath("$[0].normalizedName").doesNotExist())
                .andExpect(jsonPath("$[0].student").doesNotExist());
    }

    @Test
    void updatesOwnSkillNameAndLevel() throws Exception {
        String token = register("skill-update@example.com", "Skill Update");
        long id = addSkill(token, "Java", "BEGINNER");

        mockMvc.perform(put("/api/career/skills/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson("Spring Boot", "ADVANCED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring Boot"))
                .andExpect(jsonPath("$.level").value("ADVANCED"));
    }

    @Test
    void deletesOwnSkill() throws Exception {
        String token = register("skill-delete@example.com", "Skill Delete");
        long id = addSkill(token, "Java", "BEGINNER");

        mockMvc.perform(delete("/api/career/skills/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/career/skills").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsBlankSkillNameAndMissingOrInvalidLevel() throws Exception {
        String token = register("invalid-skill@example.com", "Invalid Skill");

        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson("   ", "BEGINNER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SkillInput("Java", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.level").exists());
        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson("Java", "EXPERT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servicePreventsExactAndCaseInsensitiveDuplicateSkills() throws Exception {
        String token = register("duplicate-skill@example.com", "Duplicate Skill");
        addSkill(token, "Java", "BEGINNER");

        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson("Java", "ADVANCED")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Skill already exists"));
        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson(" java ", "INTERMEDIATE")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson("JAVA", "ADVANCED")))
                .andExpect(status().isConflict());

        assertEquals(1, skillRepository.count());
    }

    @Test
    void databaseConstraintAlsoPreventsNormalizedSkillDuplicates() throws Exception {
        register("database-skill@example.com", "Database Skill");
        Student student = studentRepository.findByUserEmail("database-skill@example.com").orElseThrow();
        skillRepository.saveAndFlush(new Skill(student, "Java", Skill.Level.BEGINNER));

        assertThrows(DataIntegrityViolationException.class, () ->
                skillRepository.saveAndFlush(new Skill(student, " java ", Skill.Level.ADVANCED))
        );
    }

    @Test
    void anotherStudentCannotUpdateOrDeleteSkill() throws Exception {
        String owner = register("secure-skill-owner@example.com", "Owner");
        String attacker = register("secure-skill-attacker@example.com", "Attacker");
        long id = addSkill(owner, "Java", "BEGINNER");

        mockMvc.perform(put("/api/career/skills/{id}", id)
                        .header("Authorization", bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson("Tampered", "ADVANCED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Skill not found"));
        mockMvc.perform(delete("/api/career/skills/{id}", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/career/skills").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    private String register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, "Password1!", name, "CSE", 3))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private void upsertCareer(String token, String goal) throws Exception {
        mockMvc.perform(put("/api/career")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerJson(goal, List.of())))
                .andExpect(status().isOk());
    }

    private long addSkill(String token, String name, String level) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillJson(name, level)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private String careerJson(String careerGoal, List<String> interests) throws Exception {
        return objectMapper.writeValueAsString(new CareerInput(careerGoal, interests));
    }

    private String skillJson(String name, String level) throws Exception {
        return objectMapper.writeValueAsString(new SkillInput(name, level));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String email, String password, String name, String branch, int semester) {
    }

    private record CareerInput(String careerGoal, List<String> interests) {
    }

    private record SkillInput(String name, String level) {
    }
}
