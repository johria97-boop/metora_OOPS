package com.mentora.recommendation;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/generate")
    public List<RecommendationDtos.RecommendationResponse> generate(Authentication authentication) {
        return recommendationService.generate(authentication.getName());
    }

    @GetMapping
    public List<RecommendationDtos.RecommendationResponse> getRecommendations(
            Authentication authentication
    ) {
        return recommendationService.getRecommendations(authentication.getName());
    }

    @PatchMapping("/{id}/complete")
    public RecommendationDtos.RecommendationResponse complete(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return recommendationService.complete(authentication.getName(), id);
    }

    @PatchMapping("/{id}/dismiss")
    public RecommendationDtos.RecommendationResponse dismiss(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return recommendationService.dismiss(authentication.getName(), id);
    }
}
