package com.mentora.student;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public StudentDtos.Profile getCurrentProfile(Authentication authentication) {
        return studentService.getCurrentProfile(authentication.getName());
    }

    @PutMapping("/me")
    public StudentDtos.Profile updateCurrentProfile(
            Authentication authentication,
            @Valid @RequestBody StudentDtos.UpdateRequest request
    ) {
        return studentService.updateCurrentProfile(authentication.getName(), request);
    }
}

