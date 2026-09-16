package com.mentora;

import com.mentora.academic.StudyPlan;
import com.mentora.auth.User;
import com.mentora.career.CareerProfile;
import com.mentora.common.ApiException;
import com.mentora.common.Schedulable;
import com.mentora.common.Trackable;
import com.mentora.productivity.CalendarEvent;
import com.mentora.productivity.Habit;
import com.mentora.productivity.Task;
import com.mentora.student.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseThreeDomainTests {

    private static final LocalDate FIRST_DAY = LocalDate.of(2030, 1, 1);

    @Test
    void habitCompletionFollowsDeterministicStreakRules() {
        Habit habit = new Habit(null, "Read", 10);

        habit.recordCompletion(FIRST_DAY);
        assertEquals(1, habit.getCurrentStreak());

        habit.recordCompletion(FIRST_DAY.plusDays(1));
        assertEquals(2, habit.getCurrentStreak());

        habit.recordCompletion(FIRST_DAY.plusDays(1));
        assertEquals(2, habit.getCurrentStreak());

        habit.recordCompletion(FIRST_DAY.plusDays(4));
        assertEquals(1, habit.getCurrentStreak());
        assertEquals(FIRST_DAY.plusDays(4), habit.getLastCompletedDate());
    }

    @Test
    void habitRejectsCompletionBeforeItsLastCompletion() {
        Habit habit = new Habit(null, "Read", 10);
        habit.recordCompletion(FIRST_DAY);

        assertThrows(ApiException.class, () -> habit.recordCompletion(FIRST_DAY.minusDays(1)));
    }

    @Test
    void habitProgressUsesTrackableFormulaAndCapsAtOneHundred() {
        Habit habit = new Habit(null, "Read", 2);
        assertEquals(0.0, habit.getProgress());

        habit.recordCompletion(FIRST_DAY);
        assertEquals(50.0, habit.getProgress());

        habit.recordCompletion(FIRST_DAY.plusDays(1));
        habit.recordCompletion(FIRST_DAY.plusDays(2));
        assertEquals(100.0, habit.getProgress());
    }

    @Test
    void trackableReferenceWorksWithStudyPlanAndHabit() {
        Habit habit = new Habit(null, "Practice", 4);
        habit.recordCompletion(FIRST_DAY);
        StudyPlan studyPlan = new StudyPlan(null, "Algorithms", 20, 5);

        List<Trackable> trackables = List.of(studyPlan, habit);

        assertEquals(List.of(25.0, 25.0), trackables.stream().map(Trackable::getProgress).toList());
    }

    @Test
    void schedulableReferenceReschedulesTaskAndCalendarEvent() {
        LocalDate initialDate = LocalDate.now().plusDays(2);
        LocalDate newDate = LocalDate.now().plusDays(5);
        Task task = new Task(null, "Submit report", initialDate, Task.Priority.HIGH);
        CalendarEvent event = new CalendarEvent(null, "Presentation", initialDate);
        List<Schedulable> schedulables = List.of(task, event);

        schedulables.forEach(item -> item.reschedule(newDate));

        assertEquals(List.of(newDate, newDate),
                schedulables.stream().map(Schedulable::getScheduledDate).toList());
    }

    @Test
    void careerProfileDoesNotExposeMutableInterestState() {
        CareerProfile profile = new CareerProfile(null, "AI Engineer", List.of("AI", "Cyber Security"));

        assertThrows(UnsupportedOperationException.class,
                () -> profile.getInterests().add("Data Analytics"));
        assertEquals(List.of("AI", "Cyber Security"), List.copyOf(profile.getInterests()));
    }

    @Test
    void studentDomainProtectsProfileInvariantsAndNormalizesValues() {
        User user = new User("student@example.com", "encoded-password");
        Student student = new Student(user, "  Arpit  ", "  CSE  ", 3);

        assertEquals("Arpit", student.getName());
        assertEquals("CSE", student.getBranch());
        assertThrows(ApiException.class, () -> student.updateProfile(" ", "CSE", 3));
        assertThrows(ApiException.class, () -> student.updateProfile("Arpit", " ", 3));
        assertThrows(ApiException.class, () -> student.updateProfile("Arpit", "CSE", 0));
    }
}
