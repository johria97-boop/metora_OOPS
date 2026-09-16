package com.mentora.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentora.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient implements RecommendationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiClient.class);
    private static final String DEFAULT_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models";

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final String baseUrl;
    private final HttpClient httpClient;

    @Autowired
    public GeminiClient(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            @Value("${gemini.timeout-seconds:20}") int timeoutSeconds
    ) {
        this(objectMapper, apiKey, model, timeoutSeconds, DEFAULT_BASE_URL);
    }

    GeminiClient(
            ObjectMapper objectMapper,
            String apiKey,
            String model,
            int timeoutSeconds,
            String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public List<GeneratedRecommendation> generate(StudentContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Recommendation provider is not configured");
        }
        LOGGER.info("Gemini recommendation generation requested");
        try {
            String requestBody = objectMapper.writeValueAsString(buildRequest(context));
            HttpRequest request = HttpRequest.newBuilder(providerUri())
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 429) {
                LOGGER.warn("Gemini recommendation generation rate limited");
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Recommendation provider is temporarily rate limited");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Gemini recommendation generation failed with status {}",
                        response.statusCode());
                throw providerFailure();
            }
            List<GeneratedRecommendation> generated = parseResponse(response.body());
            LOGGER.info("Gemini returned {} recommendation entries", generated.size());
            return generated;
        } catch (HttpTimeoutException exception) {
            LOGGER.warn("Gemini recommendation generation timed out");
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                    "Recommendation provider timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw providerFailure();
        } catch (IOException exception) {
            LOGGER.warn("Gemini recommendation generation transport or JSON failure");
            throw providerFailure();
        }
    }

    private URI providerUri() {
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);
        return URI.create(baseUrl + "/" + encodedModel + ":generateContent");
    }

    private Map<String, Object> buildRequest(StudentContext context) throws JsonProcessingException {
        String prompt = """
                You are Mentora's recommendation engine.
                Generate concise, actionable recommendations based ONLY on the supplied student context.
                Do not invent grades, deadlines, skills, interests, goals, policies, or requirements.
                Reasonable career learning suggestions must be described as suggestions based on the career goal.
                Prioritize urgent real obligations before generic suggestions.
                Return no more than 5 recommendations.
                Allowed categories: ACADEMIC, PRODUCTIVITY, CAREER.
                Allowed priorities: URGENT, HIGH, MEDIUM, LOW.
                Every entry must contain category, priority, title, and reason.

                STUDENT_CONTEXT_JSON:
                """ + objectMapper.writeValueAsString(context);

        Map<String, Object> itemSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "category", enumStringSchema(List.of("ACADEMIC", "PRODUCTIVITY", "CAREER")),
                        "priority", enumStringSchema(List.of("URGENT", "HIGH", "MEDIUM", "LOW")),
                        "title", Map.of("type", "STRING"),
                        "reason", Map.of("type", "STRING")
                ),
                "required", List.of("category", "priority", "title", "reason")
        );
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "recommendations", Map.of(
                                "type", "ARRAY",
                                "maxItems", 5,
                                "items", itemSchema
                        )
                ),
                "required", List.of("recommendations")
        );
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );
    }

    private Map<String, Object> enumStringSchema(List<String> allowedValues) {
        return Map.of("type", "STRING", "enum", allowedValues);
    }

    private List<GeneratedRecommendation> parseResponse(String responseBody)
            throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            throw providerFailure();
        }
        String structuredText = null;
        for (JsonNode part : parts) {
            if (part.path("text").isTextual()) {
                structuredText = part.path("text").asText();
                break;
            }
        }
        if (structuredText == null || structuredText.isBlank()) {
            throw providerFailure();
        }
        JsonNode recommendations = objectMapper.readTree(stripCodeFence(structuredText))
                .path("recommendations");
        if (!recommendations.isArray()) {
            throw providerFailure();
        }
        List<GeneratedRecommendation> result = new ArrayList<>();
        for (JsonNode item : recommendations) {
            result.add(new GeneratedRecommendation(
                    item.path("category").textValue(),
                    item.path("priority").textValue(),
                    item.path("title").textValue(),
                    item.path("reason").textValue()
            ));
        }
        return result;
    }

    private String stripCodeFence(String value) {
        String clean = value.trim();
        if (clean.startsWith("```")) {
            int firstLineEnd = clean.indexOf('\n');
            int closingFence = clean.lastIndexOf("```");
            if (firstLineEnd >= 0 && closingFence > firstLineEnd) {
                return clean.substring(firstLineEnd + 1, closingFence).trim();
            }
        }
        return clean;
    }

    private ApiException providerFailure() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "Recommendation provider returned an invalid response");
    }
}
