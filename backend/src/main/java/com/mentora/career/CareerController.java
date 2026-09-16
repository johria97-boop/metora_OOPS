package com.mentora.career;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/career")
public class CareerController {

    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping
    public CareerDtos.CareerResponse getCareerProfile(Authentication authentication) {
        return careerService.getCareerProfile(authentication.getName());
    }

    @PutMapping
    public CareerDtos.CareerResponse upsertCareerProfile(
            Authentication authentication,
            @Valid @RequestBody CareerDtos.CareerRequest request
    ) {
        return careerService.upsertCareerProfile(authentication.getName(), request);
    }

    @GetMapping("/skills")
    public List<CareerDtos.SkillResponse> getSkills(Authentication authentication) {
        return careerService.getSkills(authentication.getName());
    }

    @PostMapping("/skills")
    public ResponseEntity<CareerDtos.SkillResponse> addSkill(
            Authentication authentication,
            @Valid @RequestBody CareerDtos.SkillRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careerService.addSkill(authentication.getName(), request));
    }

    @PutMapping("/skills/{id}")
    public CareerDtos.SkillResponse updateSkill(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CareerDtos.SkillRequest request
    ) {
        return careerService.updateSkill(authentication.getName(), id, request);
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(Authentication authentication, @PathVariable Long id) {
        careerService.deleteSkill(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
