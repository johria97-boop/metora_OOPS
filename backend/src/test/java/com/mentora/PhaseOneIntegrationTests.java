package com.mentora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.auth.User;
import com.mentora.auth.UserRepository;
import com.mentora.career.CareerRepository;
import com.mentora.career.SkillRepository;
import com.mentora.academic.AcademicRepository;
import com.mentora.academic.StudyPlanRepository;
import com.mentora.student.StudentRepository;
import com.mentora.productivity.CalendarEventRepository;
import com.mentora.productivity.HabitRepository;
import com.mentora.productivity.TaskRepository;
import com.mentora.recommendation.RecommendationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseOneIntegrationTests {

    private static final String TEST_SECRET =
            "dGVzdC1vbmx5LW1lbnRvcmEtand0LXNlY3JldC1tdXN0LWJlLWxvbmc=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AcademicRepository academicRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

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
    void successfulRegistrationReturnsTokenAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(" Student@Example.com ", "Password1!", "Arpit", "CSE", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.student.email").value("student@example.com"))
                .andExpect(jsonPath("$.student.name").value("Arpit"))
                .andExpect(jsonPath("$.student.passwordHash").doesNotExist());
    }

    @Test
    void duplicateEmailIsRejectedAfterNormalization() throws Exception {
        registerAndGetToken("student@example.com", "Password1!", "First", "CSE", 2);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(" STUDENT@example.com ", "Password2!", "Second", "ECE", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("not-an-email", "Password1!", "Arpit", "CSE", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    void invalidSemesterIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("student@example.com", "Password1!", "Arpit", "CSE", 13)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.semester").exists());
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        String plainPassword = "Password1!";
        registerAndGetToken("student@example.com", plainPassword, "Arpit", "CSE", 3);

        User saved = userRepository.findByEmail("student@example.com").orElseThrow();
        assertNotEquals(plainPassword, saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(plainPassword, saved.getPasswordHash()));
    }

    @Test
    void correctCredentialsLoginSuccessfully() throws Exception {
        registerAndGetToken("student@example.com", "Password1!", "Arpit", "CSE", 3);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("STUDENT@example.com", "Password1!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.student.email").value("student@example.com"));
    }

    @Test
    void incorrectPasswordIsRejected() throws Exception {
        registerAndGetToken("student@example.com", "Password1!", "Arpit", "CSE", 3);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("student@example.com", "WrongPassword")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void unknownUserLoginIsRejectedWithoutAccountDisclosure() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@example.com", "Password1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedProfileRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/student/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validTokenAllowsAccessToOwnProfile() throws Exception {
        String token = registerAndGetToken("student@example.com", "Password1!", "Arpit", "CSE", 3);

        mockMvc.perform(get("/api/student/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.name").value("Arpit"));
    }

    @Test
    void invalidAndExpiredTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/student/me").header("Authorization", bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .subject("student@example.com")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/student/me").header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void authenticatedStudentCanUpdateOwnProfile() throws Exception {
        String token = registerAndGetToken("student@example.com", "Password1!", "Arpit", "CSE", 3);

        mockMvc.perform(put("/api/student/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Arpit Sharma", "Information Technology", 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arpit Sharma"))
                .andExpect(jsonPath("$.branch").value("Information Technology"))
                .andExpect(jsonPath("$.semester").value(4));
    }

    @Test
    void meEndpointsCannotReadOrUpdateAnotherStudent() throws Exception {
        String firstToken = registerAndGetToken("first@example.com", "Password1!", "First", "CSE", 2);
        String secondToken = registerAndGetToken("second@example.com", "Password2!", "Second", "ECE", 5);

        mockMvc.perform(put("/api/student/me")
                        .header("Authorization", bearer(secondToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Second Updated", "EEE", 6)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("second@example.com"));

        mockMvc.perform(get("/api/student/me").header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("first@example.com"))
                .andExpect(jsonPath("$.name").value("First"))
                .andExpect(jsonPath("$.branch").value("CSE"))
                .andExpect(jsonPath("$.semester").value(2));

        assertFalse(studentRepository.findByUserEmail("first@example.com")
                .orElseThrow().getName().contains("Updated"));
    }

    private String registerAndGetToken(
            String email,
            String password,
            String name,
            String branch,
            int semester
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(email, password, name, branch, semester)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private String registrationJson(
            String email,
            String password,
            String name,
            String branch,
            int semester
    ) throws Exception {
        return objectMapper.writeValueAsString(new Registration(email, password, name, branch, semester));
    }

    private String loginJson(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new Login(email, password));
    }

    private String profileJson(String name, String branch, int semester) throws Exception {
        return objectMapper.writeValueAsString(new ProfileUpdate(name, branch, semester));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String email, String password, String name, String branch, int semester) {
    }

    private record Login(String email, String password) {
    }

    private record ProfileUpdate(String name, String branch, int semester) {
    }
}
