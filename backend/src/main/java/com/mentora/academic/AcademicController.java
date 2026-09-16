package com.mentora.academic;

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
@RequestMapping("/api/academic")
public class AcademicController {

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @GetMapping("/records")
    public List<AcademicDtos.AcademicRecordResponse> getRecords(Authentication authentication) {
        return academicService.getRecords(authentication.getName());
    }

    @PostMapping("/records")
    public ResponseEntity<AcademicDtos.AcademicRecordResponse> createRecord(
            Authentication authentication,
            @Valid @RequestBody AcademicDtos.AcademicRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicService.createRecord(authentication.getName(), request));
    }

    @PutMapping("/records/{id}")
    public AcademicDtos.AcademicRecordResponse updateRecord(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AcademicDtos.AcademicRecordRequest request
    ) {
        return academicService.updateRecord(authentication.getName(), id, request);
    }

    @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteRecord(Authentication authentication, @PathVariable Long id) {
        academicService.deleteRecord(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/study-plans")
    public List<AcademicDtos.StudyPlanResponse> getStudyPlans(Authentication authentication) {
        return academicService.getStudyPlans(authentication.getName());
    }

    @PostMapping("/study-plans")
    public ResponseEntity<AcademicDtos.StudyPlanResponse> createStudyPlan(
            Authentication authentication,
            @Valid @RequestBody AcademicDtos.StudyPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicService.createStudyPlan(authentication.getName(), request));
    }

    @PutMapping("/study-plans/{id}")
    public AcademicDtos.StudyPlanResponse updateStudyPlan(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AcademicDtos.StudyPlanRequest request
    ) {
        return academicService.updateStudyPlan(authentication.getName(), id, request);
    }

    @PatchMapping("/study-plans/{id}/progress")
    public AcademicDtos.StudyPlanResponse updateStudyPlanProgress(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AcademicDtos.ProgressRequest request
    ) {
        return academicService.updateStudyPlanProgress(authentication.getName(), id, request);
    }

    @DeleteMapping("/study-plans/{id}")
    public ResponseEntity<Void> deleteStudyPlan(Authentication authentication, @PathVariable Long id) {
        academicService.deleteStudyPlan(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
