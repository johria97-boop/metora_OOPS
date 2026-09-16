package com.mentora.career;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class CareerDtos {

    private CareerDtos() {
    }

    public record CareerRequest(
            @NotBlank @Size(max = 150) String careerGoal,
            @NotNull @Size(max = CareerProfile.MAX_INTERESTS)
            List<@Valid @NotBlank @Size(max = CareerProfile.MAX_INTEREST_LENGTH) String> interests
    ) {
    }

    public record CareerResponse(Long id, String careerGoal, List<String> interests) {

        public static CareerResponse empty() {
            return new CareerResponse(null, "", List.of());
        }

        public static CareerResponse from(CareerProfile profile) {
            return new CareerResponse(
                    profile.getId(), profile.getCareerGoal(), List.copyOf(profile.getInterests())
            );
        }
    }

    public record SkillRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Skill.Level level
    ) {
    }

    public record SkillResponse(Long id, String name, Skill.Level level) {

        public static SkillResponse from(Skill skill) {
            return new SkillResponse(skill.getId(), skill.getName(), skill.getLevel());
        }
    }
}
