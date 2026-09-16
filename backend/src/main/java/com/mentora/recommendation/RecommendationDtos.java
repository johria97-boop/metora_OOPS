package com.mentora.recommendation;

import java.time.Instant;

public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    public record RecommendationResponse(
            Long id,
            Recommendation.Category category,
            Recommendation.Priority priority,
            String title,
            String reason,
            Recommendation.Status status,
            Instant createdAt
    ) {

        public static RecommendationResponse from(Recommendation recommendation) {
            return new RecommendationResponse(
                    recommendation.getId(),
                    recommendation.getCategory(),
                    recommendation.getPriority(),
                    recommendation.getTitle(),
                    recommendation.getReason(),
                    recommendation.getStatus(),
                    recommendation.getCreatedAt()
            );
        }
    }
}
