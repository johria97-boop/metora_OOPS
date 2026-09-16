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
import com.mentora.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

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
class PhaseThreeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void createsValidTaskWithoutExposingStudent() throws Exception {
        String token = register("task@example.com", "Task Student");

        mockMvc.perform(post("/api/productivity/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskCreateJson("Submit report", future(2), "HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Submit report"))
                .andExpect(jsonPath("$.dueDate").value(future(2).toString()))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.student").doesNotExist());
    }

    @Test
    void rejectsInvalidTaskTitleDateAndPriority() throws Exception {
        String token = register("invalid-task@example.com", "Invalid Task");

        mockMvc.perform(post("/api/productivity/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskCreateJson("   ", future(1), "HIGH")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.title").exists());
        mockMvc.perform(post("/api/productivity/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskCreateJson("Late", LocalDate.now().minusDays(1), "LOW")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.dueDate").exists());
        mockMvc.perform(post("/api/productivity/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskCreateJson("Invalid priority", future(1), "URGENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsOnlyOwnTasks() throws Exception {
        String owner = register("task-owner@example.com", "Owner");
        String other = register("task-other@example.com", "Other");
        createTask(owner, "Owner task");
        createTask(other, "Other task");

        mockMvc.perform(get("/api/productivity/tasks").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Owner task"));
    }

    @Test
    void updatesCompletesAndReschedulesOwnTask() throws Exception {
        String token = register("task-actions@example.com", "Task Actions");
        long id = createTask(token, "Draft report");

        mockMvc.perform(put("/api/productivity/tasks/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskUpdateJson("Final report", "HIGH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final report"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
        mockMvc.perform(patch("/api/productivity/tasks/{id}/complete", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
        mockMvc.perform(patch("/api/productivity/tasks/{id}/reschedule", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson(future(8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value(future(8).toString()));
    }

    @Test
    void deletesOwnTask() throws Exception {
        String token = register("task-delete@example.com", "Task Delete");
        long id = createTask(token, "Disposable task");

        mockMvc.perform(delete("/api/productivity/tasks/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/productivity/tasks").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anotherStudentCannotMutateTask() throws Exception {
        String owner = register("secure-task-owner@example.com", "Owner");
        String attacker = register("secure-task-attacker@example.com", "Attacker");
        long id = createTask(owner, "Private task");

        mockMvc.perform(patch("/api/productivity/tasks/{id}/complete", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
        mockMvc.perform(delete("/api/productivity/tasks/{id}", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/productivity/tasks").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void unauthenticatedTaskRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/productivity/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsHabitAndRejectsNonPositiveTarget() throws Exception {
        String token = register("habit@example.com", "Habit Student");

        mockMvc.perform(post("/api/productivity/habits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(habitJson("Read", 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.progress").value(0.0))
                .andExpect(jsonPath("$.student").doesNotExist());
        mockMvc.perform(post("/api/productivity/habits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(habitJson("Exercise", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.targetStreak").exists());
    }

    @Test
    void recordsFirstHabitCompletionOnlyOncePerDay() throws Exception {
        String token = register("habit-complete@example.com", "Habit Complete");
        long id = createHabit(token, "Read", 5);

        mockMvc.perform(patch("/api/productivity/habits/{id}/complete", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.lastCompletedDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.progress").value(20.0));
        mockMvc.perform(patch("/api/productivity/habits/{id}/complete", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1));
    }

    @Test
    void listsUpdatesAndDeletesOwnHabit() throws Exception {
        String token = register("habit-actions@example.com", "Habit Actions");
        long id = createHabit(token, "Read", 5);

        mockMvc.perform(get("/api/productivity/habits").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(put("/api/productivity/habits/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(habitJson("Read books", 12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Read books"))
                .andExpect(jsonPath("$.targetStreak").value(12));
        mockMvc.perform(delete("/api/productivity/habits/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherStudentCannotModifyHabit() throws Exception {
        String owner = register("secure-habit-owner@example.com", "Owner");
        String attacker = register("secure-habit-attacker@example.com", "Attacker");
        long id = createHabit(owner, "Private habit", 7);

        mockMvc.perform(put("/api/productivity/habits/{id}", id)
                        .header("Authorization", bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(habitJson("Tampered", 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Habit not found"));
        mockMvc.perform(patch("/api/productivity/habits/{id}/complete", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsCalendarEventAndRejectsInvalidInput() throws Exception {
        String token = register("event@example.com", "Event Student");

        mockMvc.perform(post("/api/productivity/calendar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Exam", future(4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Exam"))
                .andExpect(jsonPath("$.eventDate").value(future(4).toString()))
                .andExpect(jsonPath("$.student").doesNotExist());
        mockMvc.perform(post("/api/productivity/calendar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("   ", LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.title").exists())
                .andExpect(jsonPath("$.details.eventDate").exists());
    }

    @Test
    void listsUpdatesReschedulesAndDeletesOwnCalendarEvent() throws Exception {
        String token = register("event-actions@example.com", "Event Actions");
        long id = createEvent(token, "Exam");

        mockMvc.perform(get("/api/productivity/calendar").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(put("/api/productivity/calendar/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EventUpdate("Final exam"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final exam"));
        mockMvc.perform(patch("/api/productivity/calendar/{id}/reschedule", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson(future(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventDate").value(future(10).toString()));
        mockMvc.perform(delete("/api/productivity/calendar/{id}", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherStudentCannotModifyCalendarEvent() throws Exception {
        String owner = register("secure-event-owner@example.com", "Owner");
        String attacker = register("secure-event-attacker@example.com", "Attacker");
        long id = createEvent(owner, "Private event");

        mockMvc.perform(patch("/api/productivity/calendar/{id}/reschedule", id)
                        .header("Authorization", bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleJson(future(6))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Calendar event not found"));
        mockMvc.perform(delete("/api/productivity/calendar/{id}", id)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isNotFound());
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

    private long createTask(String token, String title) throws Exception {
        return idFrom(mockMvc.perform(post("/api/productivity/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskCreateJson(title, future(2), "MEDIUM")))
                .andExpect(status().isCreated()).andReturn());
    }

    private long createHabit(String token, String name, int target) throws Exception {
        return idFrom(mockMvc.perform(post("/api/productivity/habits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(habitJson(name, target)))
                .andExpect(status().isCreated()).andReturn());
    }

    private long createEvent(String token, String title) throws Exception {
        return idFrom(mockMvc.perform(post("/api/productivity/calendar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(title, future(3))))
                .andExpect(status().isCreated()).andReturn());
    }

    private long idFrom(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private String taskCreateJson(String title, LocalDate dueDate, String priority) throws Exception {
        return objectMapper.writeValueAsString(new TaskCreate(title, dueDate, priority));
    }

    private String taskUpdateJson(String title, String priority) throws Exception {
        return objectMapper.writeValueAsString(new TaskUpdate(title, priority));
    }

    private String habitJson(String name, int targetStreak) throws Exception {
        return objectMapper.writeValueAsString(new HabitInput(name, targetStreak));
    }

    private String eventJson(String title, LocalDate eventDate) throws Exception {
        return objectMapper.writeValueAsString(new EventCreate(title, eventDate));
    }

    private String rescheduleJson(LocalDate scheduledDate) throws Exception {
        return objectMapper.writeValueAsString(new Reschedule(scheduledDate));
    }

    private LocalDate future(long days) {
        return LocalDate.now().plusDays(days);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Registration(String email, String password, String name, String branch, int semester) {
    }

    private record TaskCreate(String title, LocalDate dueDate, String priority) {
    }

    private record TaskUpdate(String title, String priority) {
    }

    private record HabitInput(String name, int targetStreak) {
    }

    private record EventCreate(String title, LocalDate eventDate) {
    }

    private record EventUpdate(String title) {
    }

    private record Reschedule(LocalDate scheduledDate) {
    }
}
