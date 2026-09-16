package com.mentora.career;

import com.mentora.common.ApiException;
import com.mentora.student.Student;
import com.mentora.student.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CareerService {

    private final CareerRepository careerRepository;
    private final SkillRepository skillRepository;
    private final StudentService studentService;

    public CareerService(
            CareerRepository careerRepository,
            SkillRepository skillRepository,
            StudentService studentService
    ) {
        this.careerRepository = careerRepository;
        this.skillRepository = skillRepository;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public CareerDtos.CareerResponse getCareerProfile(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return careerRepository.findByStudent(student)
                .map(CareerDtos.CareerResponse::from)
                .orElseGet(CareerDtos.CareerResponse::empty);
    }

    @Transactional
    public CareerDtos.CareerResponse upsertCareerProfile(
            String email,
            CareerDtos.CareerRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        CareerProfile profile = careerRepository.findByStudent(student)
                .orElseGet(() -> new CareerProfile(student, request.careerGoal(), request.interests()));
        if (profile.getId() != null) {
            profile.update(request.careerGoal(), request.interests());
        }
        return CareerDtos.CareerResponse.from(careerRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<CareerDtos.SkillResponse> getSkills(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return skillRepository.findAllByStudentOrderByNameAsc(student)
                .stream().map(CareerDtos.SkillResponse::from).toList();
    }

    @Transactional
    public CareerDtos.SkillResponse addSkill(String email, CareerDtos.SkillRequest request) {
        Student student = studentService.requireCurrentStudent(email);
        String normalizedName = Skill.normalizeName(request.name());
        rejectDuplicateSkill(student, normalizedName, null);
        Skill skill = new Skill(student, request.name(), request.level());
        return CareerDtos.SkillResponse.from(skillRepository.save(skill));
    }

    @Transactional
    public CareerDtos.SkillResponse updateSkill(
            String email,
            Long id,
            CareerDtos.SkillRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        Skill skill = requireSkill(id, student);
        String normalizedName = Skill.normalizeName(request.name());
        rejectDuplicateSkill(student, normalizedName, id);
        skill.rename(request.name());
        skill.updateLevel(request.level());
        return CareerDtos.SkillResponse.from(skill);
    }

    @Transactional
    public void deleteSkill(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        skillRepository.delete(requireSkill(id, student));
    }

    private void rejectDuplicateSkill(Student student, String normalizedName, Long excludedId) {
        boolean duplicate = excludedId == null
                ? skillRepository.existsByStudentAndNormalizedName(student, normalizedName)
                : skillRepository.existsByStudentAndNormalizedNameAndIdNot(
                        student, normalizedName, excludedId
                );
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "Skill already exists");
        }
    }

    private Skill requireSkill(Long id, Student student) {
        return skillRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found"));
    }
}
