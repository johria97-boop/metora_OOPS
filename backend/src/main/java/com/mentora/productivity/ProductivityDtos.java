package com.mentora.productivity;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class ProductivityDtos {

    private ProductivityDtos() {
    }

    public record TaskCreateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull @FutureOrPresent LocalDate dueDate,
            @NotNull Task.Priority priority
    ) {
    }

    public record TaskUpdateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull Task.Priority priority
    ) {
    }

    public record RescheduleRequest(
            @NotNull @FutureOrPresent LocalDate scheduledDate
    ) {
    }

    public record TaskResponse(
            Long id,
            String title,
            LocalDate dueDate,
            Task.Priority priority,
            boolean completed
    ) {

        public static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(), task.getTitle(), task.getDueDate(), task.getPriority(), task.isCompleted()
            );
        }
    }

    public record HabitRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @Positive Integer targetStreak
    ) {
    }

    public record HabitResponse(
            Long id,
            String name,
            int targetStreak,
            int currentStreak,
            LocalDate lastCompletedDate,
            double progress
    ) {

        public static HabitResponse from(Habit habit) {
            return new HabitResponse(
                    habit.getId(),
                    habit.getName(),
                    habit.getTargetStreak(),
                    habit.getCurrentStreak(),
                    habit.getLastCompletedDate(),
                    habit.getProgress()
            );
        }
    }

    public record CalendarEventCreateRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull @FutureOrPresent LocalDate eventDate
    ) {
    }

    public record CalendarEventUpdateRequest(
            @NotBlank @Size(max = 150) String title
    ) {
    }

    public record CalendarEventResponse(Long id, String title, LocalDate eventDate) {

        public static CalendarEventResponse from(CalendarEvent event) {
            return new CalendarEventResponse(event.getId(), event.getTitle(), event.getEventDate());
        }
    }
}
