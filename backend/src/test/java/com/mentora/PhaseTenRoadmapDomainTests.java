package com.mentora;

import com.mentora.academic.StudyPlan;
import com.mentora.auth.User;
import com.mentora.common.Trackable;
import com.mentora.productivity.Habit;
import com.mentora.roadmap.Roadmap;
import com.mentora.roadmap.RoadmapStep;
import com.mentora.student.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseTenRoadmapDomainTests {

    @Test
    void newRoadmapImplementsTrackableAndStartsAtZeroProgress() {
        Roadmap roadmap = roadmap();

        assertEquals(0.0, roadmap.getProgress());
        assertEquals(0, roadmap.getCompletedStepCount());
    }

    @Test
    void roadmapProgressCountsOnlyCompletedStepsAndCapsAtOneHundred() {
        Roadmap roadmap = roadmapWithThreeSteps();
        roadmap.getSteps().get(0).updateStatus(RoadmapStep.Status.IN_PROGRESS);
        assertEquals(0.0, roadmap.getProgress());

        roadmap.getSteps().get(0).updateStatus(RoadmapStep.Status.COMPLETED);
        assertEquals(100.0 / 3.0, roadmap.getProgress());
        roadmap.getSteps().forEach(step -> step.updateStatus(RoadmapStep.Status.COMPLETED));
        assertEquals(100.0, roadmap.getProgress());
    }

    @Test
    void trackablePolymorphismIncludesStudyPlanHabitAndRoadmap() {
        StudyPlan studyPlan = new StudyPlan(null, "Algorithms", 20, 5);
        Habit habit = new Habit(null, "Practice", 4);
        habit.recordCompletion(LocalDate.of(2030, 1, 1));
        Roadmap roadmap = roadmapWithThreeSteps();
        roadmap.getSteps().get(0).updateStatus(RoadmapStep.Status.COMPLETED);

        List<Trackable> trackables = List.of(studyPlan, habit, roadmap);

        assertEquals(List.of(25.0, 25.0, 100.0 / 3.0),
                trackables.stream().map(Trackable::getProgress).toList());
    }

    @Test
    void roadmapOwnsAnUnmodifiableStepCollection() {
        Roadmap roadmap = roadmapWithThreeSteps();

        assertThrows(UnsupportedOperationException.class,
                () -> roadmap.getSteps().clear());
        assertEquals(3, roadmap.getSteps().size());
    }

    private Roadmap roadmapWithThreeSteps() {
        Roadmap roadmap = roadmap();
        roadmap.addStep(1, "Foundations", 1, "One", "First step", 2);
        roadmap.addStep(1, "Foundations", 2, "Two", "Second step", 2);
        roadmap.addStep(2, "Application", 3, "Three", "Third step", 3);
        return roadmap;
    }

    private Roadmap roadmap() {
        User user = new User("roadmap-domain@example.com", "encoded-password");
        Student student = new Student(user, "Roadmap Student", "CSE", 3);
        return new Roadmap(
                student, "Java", "A practical roadmap", Roadmap.Level.BEGINNER, 8
        );
    }
}
