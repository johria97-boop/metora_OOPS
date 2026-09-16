package com.mentora.dashboard;

import com.mentora.career.Skill;
import com.mentora.productivity.Task;
import com.mentora.recommendation.Recommendation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardResponse(
            StudentSummary student,
            AcademicSummary academics,
            StudyPlanSummary studyPlans,
            TaskSummary tasks,
            HabitSummary habits,
            CalendarSummary calendar,
            CareerSummary career,
            RecommendationSummary recommendations,
            RoadmapSummary roadmaps
    ) {
    }

    public record StudentSummary(String name, String branch, int semester) {
    }

    public record SubjectScore(String subject, double score, int semester) {
    }

    public record AcademicSummary(
            int subjectCount,
            Double averageScore,
            SubjectScore strongestSubject,
            SubjectScore attentionSubject,
            List<SubjectScore> subjects
    ) {
    }

    public record StudyPlanItem(
            String subject,
            double targetHours,
            double completedHours,
            double progress
    ) {
    }

    public record StudyPlanSummary(
            int totalPlans,
            Double averageProgress,
            List<StudyPlanItem> plans
    ) {
    }

    public record TaskItem(String title, LocalDate dueDate, Task.Priority priority) {
    }

    public record TaskSummary(
            int pendingTaskCount,
            int highPriorityPendingCount,
            List<TaskItem> nextDueTasks
    ) {
    }

    public record HabitItem(
            String name,
            int targetStreak,
            int currentStreak,
            double progress
    ) {
    }

    public record HabitSummary(int totalHabits, List<HabitItem> habits) {
    }

    public record CalendarItem(String title, LocalDate eventDate) {
    }

    public record CalendarSummary(
            int upcomingEventCount,
            List<CalendarItem> upcomingEvents
    ) {
    }

    public record SkillItem(String name, Skill.Level level) {
    }

    public record CareerSummary(
            boolean setupNeeded,
            String careerGoal,
            List<String> interests,
            int skillCount,
            List<SkillItem> skills
    ) {
    }

    public record RecommendationItem(
            Recommendation.Category category,
            Recommendation.Priority priority,
            String title,
            String reason,
            Instant createdAt
    ) {
    }

    public record RecommendationSummary(
            int activeCount,
            List<RecommendationItem> activeRecommendations
    ) {
    }

    public record RoadmapItem(
            Long id,
            String courseName,
            double progress,
            int completedSteps,
            int totalSteps
    ) {
    }

    public record RoadmapSummary(
            int totalRoadmaps,
            int activeRoadmapCount,
            RoadmapItem currentRoadmap
    ) {
    }
}
