package com.mentora.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class StudentDtos {

    private StudentDtos() {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String branch,
            @Min(1) @Max(12) int semester
    ) {
    }

    public record Profile(Long id, String email, String name, String branch, int semester) {

        public static Profile from(Student student) {
            return new Profile(
                    student.getId(),
                    student.getUser().getEmail(),
                    student.getName(),
                    student.getBranch(),
                    student.getSemester()
            );
        }
    }
}

