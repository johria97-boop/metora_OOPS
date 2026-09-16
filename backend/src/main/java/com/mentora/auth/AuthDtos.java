package com.mentora.auth;

import com.mentora.student.StudentDtos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String branch,
            @Min(1) @Max(12) int semester
    ) {
        public RegisterRequest {
            email = email == null ? null : email.trim();
        }

        @Override
        public String toString() {
            return "RegisterRequest[credentials=[REDACTED], profile=[REDACTED]]";
        }
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password
    ) {
        public LoginRequest {
            email = email == null ? null : email.trim();
        }

        @Override
        public String toString() {
            return "LoginRequest[credentials=[REDACTED]]";
        }
    }

    public record AuthResponse(String token, StudentDtos.Profile student) {
        @Override
        public String toString() {
            return "AuthResponse[token=[REDACTED], student=[REDACTED]]";
        }
    }
}
