package com.mentora.recommendation;

import java.time.LocalDate;
import java.util.List;

public interface RecommendationProvider {

    List<GeneratedRecommendation> generate(StudentContext context);

    record StudentContext(
            int semester,
            String branch,
            List<AcademicItem> academicRecords,
            List<StudyPlanItem> studyPlans,
            List<TaskItem> pendingTasks,
            List<HabitItem> habits,
            List<CalendarItem> calendarEvents,
            CareerItem career,
            List<SkillItem> skills
    ) {
    }

    record AcademicItem(String subject, double score, int semester) {
    }

    record StudyPlanItem(
            String subject,
            double targetHours,
            double completedHours,
            double progress
    ) {
    }

    record TaskItem(String title, LocalDate dueDate, String priority) {
    }

    record HabitItem(String name, int targetStreak, int currentStreak, double progress) {
    }

    record CalendarItem(String title, LocalDate eventDate) {
    }

    record CareerItem(String careerGoal, List<String> interests) {
    }

    record SkillItem(String name, String level) {
    }

    record GeneratedRecommendation(String category, String priority, String title, String reason) {
    }
}
