package com.mentora.roadmap;

import com.mentora.common.ApiException;
import com.mentora.student.Student;
import com.mentora.student.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoadmapService {

    static final int MIN_STEPS = 3;
    static final int MAX_STEPS = 20;

    private final RoadmapRepository roadmapRepository;
    private final RoadmapProvider roadmapProvider;
    private final StudentService studentService;

    public RoadmapService(
            RoadmapRepository roadmapRepository,
            RoadmapProvider roadmapProvider,
            StudentService studentService
    ) {
        this.roadmapRepository = roadmapRepository;
        this.roadmapProvider = roadmapProvider;
        this.studentService = studentService;
    }

    @Transactional
    public RoadmapDtos.RoadmapResponse generate(
            String email,
            RoadmapDtos.GenerateRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        String courseName = cleanCourseName(request.courseName());
        String normalized = Roadmap.normalizeCourseName(courseName);
        boolean activeExists = roadmapRepository
                .findByStudentAndNormalizedCourseName(student, normalized)
                .stream().anyMatch(roadmap -> !roadmap.isCompleted());
        if (activeExists) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "An active roadmap for this course already exists");
        }
        Roadmap.Level level = request.currentLevel() == null
                ? Roadmap.Level.BEGINNER : request.currentLevel();
        validateTargetWeeks(request.targetWeeks());

        RoadmapProvider.GeneratedRoadmap generated;
        try {
            generated = roadmapProvider.generate(new RoadmapProvider.RoadmapRequest(
                    courseName, level, request.targetWeeks()
            ));
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Roadmap provider is temporarily unavailable");
        }

        Roadmap roadmap = buildValidatedRoadmap(
                student, courseName, level, request.targetWeeks(), generated
        );
        return RoadmapDtos.RoadmapResponse.from(roadmapRepository.save(roadmap));
    }

    @Transactional(readOnly = true)
    public List<RoadmapDtos.RoadmapResponse> getRoadmaps(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return roadmapRepository.findByStudentOrderByCreatedAtDesc(student)
                .stream().map(RoadmapDtos.RoadmapResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RoadmapDtos.RoadmapResponse getRoadmap(String email, Long id) {
        return RoadmapDtos.RoadmapResponse.from(requireOwnedRoadmap(email, id));
    }

    @Transactional
    public RoadmapDtos.RoadmapResponse updateStepStatus(
            String email,
            Long roadmapId,
            Long stepId,
            RoadmapDtos.StatusRequest request
    ) {
        Roadmap roadmap = requireOwnedRoadmap(email, roadmapId);
        roadmap.requireStep(stepId).updateStatus(request.status());
        return RoadmapDtos.RoadmapResponse.from(roadmap);
    }

    @Transactional
    public void deleteRoadmap(String email, Long id) {
        roadmapRepository.delete(requireOwnedRoadmap(email, id));
    }

    private Roadmap requireOwnedRoadmap(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        return roadmapRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Roadmap not found"));
    }

    private Roadmap buildValidatedRoadmap(
            Student student,
            String courseName,
            Roadmap.Level level,
            Integer targetWeeks,
            RoadmapProvider.GeneratedRoadmap generated
    ) {
        if (generated == null || generated.summary() == null || generated.summary().isBlank()) {
            throw invalidProviderOutput();
        }
        String summary = generated.summary().trim().replaceAll("\\s+", " ");
        if (summary.length() > 1000 || generated.steps() == null
                || generated.steps().size() < MIN_STEPS
                || generated.steps().size() > MAX_STEPS) {
            throw invalidProviderOutput();
        }

        List<IndexedStep> sorted = new ArrayList<>();
        Map<Integer, String> phaseTitles = new LinkedHashMap<>();
        for (int index = 0; index < generated.steps().size(); index++) {
            RoadmapProvider.GeneratedStep step = generated.steps().get(index);
            validateStep(step);
            String cleanPhaseTitle = cleanProviderText(step.phaseTitle(), 100);
            String existingTitle = phaseTitles.putIfAbsent(step.phaseNumber(), cleanPhaseTitle);
            if (existingTitle != null && !existingTitle.equals(cleanPhaseTitle)) {
                throw invalidProviderOutput();
            }
            sorted.add(new IndexedStep(step, index));
        }
        sorted.sort(Comparator
                .comparingInt((IndexedStep item) -> item.step().phaseNumber())
                .thenComparingInt(item -> item.step().position())
                .thenComparingInt(IndexedStep::originalIndex));

        Roadmap roadmap = new Roadmap(student, courseName, summary, level, targetWeeks);
        Map<Integer, Integer> normalizedPhases = new LinkedHashMap<>();
        for (int index = 0; index < sorted.size(); index++) {
            RoadmapProvider.GeneratedStep step = sorted.get(index).step();
            int phaseNumber = normalizedPhases.computeIfAbsent(
                    step.phaseNumber(), ignored -> normalizedPhases.size() + 1
            );
            roadmap.addStep(
                    phaseNumber,
                    cleanProviderText(step.phaseTitle(), 100),
                    index + 1,
                    cleanProviderText(step.title(), 150),
                    cleanProviderText(step.description(), 500),
                    step.estimatedHours()
            );
        }
        return roadmap;
    }

    private void validateStep(RoadmapProvider.GeneratedStep step) {
        if (step == null || step.phaseNumber() == null || step.phaseNumber() <= 0
                || step.position() == null || step.position() <= 0
                || step.estimatedHours() == null
                || step.estimatedHours() < 1 || step.estimatedHours() > 200) {
            throw invalidProviderOutput();
        }
        cleanProviderText(step.phaseTitle(), 100);
        cleanProviderText(step.title(), 150);
        cleanProviderText(step.description(), 500);
    }

    private String cleanProviderText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw invalidProviderOutput();
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() > maxLength) {
            throw invalidProviderOutput();
        }
        return clean;
    }

    private String cleanCourseName(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Course name is required");
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Course name must not exceed 120 characters");
        }
        return clean;
    }

    private void validateTargetWeeks(Integer targetWeeks) {
        if (targetWeeks != null && (targetWeeks < 1 || targetWeeks > 52)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Target weeks must be between 1 and 52");
        }
    }

    private ApiException invalidProviderOutput() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "Roadmap provider returned an invalid response");
    }

    private record IndexedStep(RoadmapProvider.GeneratedStep step, int originalIndex) {
    }
}
