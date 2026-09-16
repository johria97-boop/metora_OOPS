package com.mentora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.academic.AcademicRecord;
import com.mentora.academic.StudyPlan;
import com.mentora.auth.AuthDtos;
import com.mentora.auth.JwtService;
import com.mentora.auth.User;
import com.mentora.common.ApiException;
import com.mentora.common.SecurityConfig;
import com.mentora.productivity.CalendarEvent;
import com.mentora.productivity.Habit;
import com.mentora.productivity.Task;
import com.mentora.student.Student;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseEightSecurityTests {

    private static final String TEST_SECRET =
            "dGVzdC1vbmx5LW1lbnRvcmEtand0LXNlY3JldC1tdXN0LWJlLWxvbmc=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void malformedAuthorizationHeaderReturnsControlledUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/api/student/me").header(HttpHeaders.AUTHORIZATION, "Bearer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/student/me"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void tokenWithInvalidSignatureIsRejected() throws Exception {
        String wrongSecret = Base64.getEncoder().encodeToString(
                "test-only-invalid-signature-key-material".getBytes(StandardCharsets.UTF_8)
        );
        JwtService wrongSigner = new JwtService(wrongSecret, 60_000);
        String token = wrongSigner.generateToken(new User("signature@example.com", "test-hash"));

        assertInvalidToken(token);
    }

    @Test
    void tokenWithoutSubjectIsRejected() throws Exception {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();

        assertInvalidToken(token);
    }

    @Test
    void tokenForNonexistentUserIsRejected() throws Exception {
        String token = jwtService.generateToken(new User("deleted@example.com", "test-hash"));

        assertInvalidToken(token);
    }

    @Test
    void onlyExactAuthenticationPostRoutesArePublic() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        mockMvc.perform(post("/api/auth/not-a-route"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void securityHeadersAreAppliedToAuthenticatedResponses() throws Exception {
        String token = registerAndGetToken("headers@example.com");

        mockMvc.perform(get("/api/student/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void corsAllowsConfiguredOriginAndRejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/student/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"));

        mockMvc.perform(options("/api/student/me")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedJsonAndInvalidEnumReturnControlledBadRequest() throws Exception {
        String token = registerAndGetToken("parsing@example.com");

        mockMvc.perform(post("/api/productivity/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is invalid"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(content().string(not(containsString("JsonMappingException"))));

        mockMvc.perform(post("/api/productivity/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Review","dueDate":"2099-01-01","priority":"SUPER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is invalid"));
    }

    @Test
    void oversizedInputIsRejectedAtApiBoundary() throws Exception {
        String token = registerAndGetToken("oversized@example.com");
        String oversizedName = "x".repeat(101);

        mockMvc.perform(put("/api/student/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProfileUpdate(oversizedName, "CSE", 3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void unsafeConfigurationValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(TEST_SECRET, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityConfig().corsConfigurationSource("*"));
    }

    @Test
    void authenticationDtosRedactPasswordsAndTokensFromDiagnosticText() {
        String password = "plain-password-marker";
        String token = "jwt-token-marker";

        assertFalse(new AuthDtos.RegisterRequest(
                "student@example.com", password, "Student", "CSE", 3
        ).toString().contains(password));
        assertFalse(new AuthDtos.LoginRequest("student@example.com", password)
                .toString().contains(password));
        assertFalse(new AuthDtos.AuthResponse(token, null).toString().contains(token));
    }

    @Test
    void entityMethodsProtectTextInvariants() {
        LocalDate today = LocalDate.now();
        Student student = new Student(new User("domain@example.com", "test-hash"), "Student", "CSE", 3);
        assertThrows(ApiException.class,
                () -> new AcademicRecord(student, "x".repeat(101), 80, 1));
        assertThrows(ApiException.class,
                () -> new StudyPlan(student, "x".repeat(101), 2, 0));
        assertThrows(ApiException.class,
                () -> new Task(student, "x".repeat(151), today, Task.Priority.HIGH));
        assertThrows(ApiException.class,
                () -> new Habit(student, "x".repeat(101), 7));
        assertThrows(ApiException.class,
                () -> new CalendarEvent(student, "x".repeat(151), today));
    }

    private void assertInvalidToken(String token) throws Exception {
        mockMvc.perform(get("/api/student/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"))
                .andExpect(jsonPath("$.details").isMap());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration(email, "Password1!", "Student", "CSE", 3))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String email, String password, String name, String branch, int semester) {
    }

    private record ProfileUpdate(String name, String branch, int semester) {
    }
}
