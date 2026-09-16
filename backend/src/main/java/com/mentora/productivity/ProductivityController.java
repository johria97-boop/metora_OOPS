package com.mentora.productivity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productivity")
public class ProductivityController {

    private final ProductivityService productivityService;

    public ProductivityController(ProductivityService productivityService) {
        this.productivityService = productivityService;
    }

    @GetMapping("/tasks")
    public List<ProductivityDtos.TaskResponse> getTasks(Authentication authentication) {
        return productivityService.getTasks(authentication.getName());
    }

    @PostMapping("/tasks")
    public ResponseEntity<ProductivityDtos.TaskResponse> createTask(
            Authentication authentication,
            @Valid @RequestBody ProductivityDtos.TaskCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productivityService.createTask(authentication.getName(), request));
    }

    @PutMapping("/tasks/{id}")
    public ProductivityDtos.TaskResponse updateTask(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductivityDtos.TaskUpdateRequest request
    ) {
        return productivityService.updateTask(authentication.getName(), id, request);
    }

    @PatchMapping("/tasks/{id}/complete")
    public ProductivityDtos.TaskResponse completeTask(Authentication authentication, @PathVariable Long id) {
        return productivityService.completeTask(authentication.getName(), id);
    }

    @PatchMapping("/tasks/{id}/reschedule")
    public ProductivityDtos.TaskResponse rescheduleTask(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductivityDtos.RescheduleRequest request
    ) {
        return productivityService.rescheduleTask(authentication.getName(), id, request);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(Authentication authentication, @PathVariable Long id) {
        productivityService.deleteTask(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/habits")
    public List<ProductivityDtos.HabitResponse> getHabits(Authentication authentication) {
        return productivityService.getHabits(authentication.getName());
    }

    @PostMapping("/habits")
    public ResponseEntity<ProductivityDtos.HabitResponse> createHabit(
            Authentication authentication,
            @Valid @RequestBody ProductivityDtos.HabitRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productivityService.createHabit(authentication.getName(), request));
    }

    @PutMapping("/habits/{id}")
    public ProductivityDtos.HabitResponse updateHabit(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductivityDtos.HabitRequest request
    ) {
        return productivityService.updateHabit(authentication.getName(), id, request);
    }

    @PatchMapping("/habits/{id}/complete")
    public ProductivityDtos.HabitResponse completeHabit(Authentication authentication, @PathVariable Long id) {
        return productivityService.completeHabit(authentication.getName(), id);
    }

    @DeleteMapping("/habits/{id}")
    public ResponseEntity<Void> deleteHabit(Authentication authentication, @PathVariable Long id) {
        productivityService.deleteHabit(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendar")
    public List<ProductivityDtos.CalendarEventResponse> getCalendarEvents(Authentication authentication) {
        return productivityService.getCalendarEvents(authentication.getName());
    }

    @PostMapping("/calendar")
    public ResponseEntity<ProductivityDtos.CalendarEventResponse> createCalendarEvent(
            Authentication authentication,
            @Valid @RequestBody ProductivityDtos.CalendarEventCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productivityService.createCalendarEvent(authentication.getName(), request));
    }

    @PutMapping("/calendar/{id}")
    public ProductivityDtos.CalendarEventResponse updateCalendarEvent(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductivityDtos.CalendarEventUpdateRequest request
    ) {
        return productivityService.updateCalendarEvent(authentication.getName(), id, request);
    }

    @PatchMapping("/calendar/{id}/reschedule")
    public ProductivityDtos.CalendarEventResponse rescheduleCalendarEvent(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductivityDtos.RescheduleRequest request
    ) {
        return productivityService.rescheduleCalendarEvent(authentication.getName(), id, request);
    }

    @DeleteMapping("/calendar/{id}")
    public ResponseEntity<Void> deleteCalendarEvent(Authentication authentication, @PathVariable Long id) {
        productivityService.deleteCalendarEvent(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
