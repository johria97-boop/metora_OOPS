package com.mentora.academic;

import com.mentora.common.ApiException;
import com.mentora.student.Student;
import com.mentora.student.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicService {

    private final AcademicRepository academicRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentService studentService;

    public AcademicService(
            AcademicRepository academicRepository,
            StudyPlanRepository studyPlanRepository,
            StudentService studentService
    ) {
        this.academicRepository = academicRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.AcademicRecordResponse> getRecords(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return academicRepository.findAllByStudentOrderBySemesterAscSubjectAsc(student)
                .stream().map(AcademicDtos.AcademicRecordResponse::from).toList();
    }

    @Transactional
    public AcademicDtos.AcademicRecordResponse createRecord(
            String email,
            AcademicDtos.AcademicRecordRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        AcademicRecord record = new AcademicRecord(
                student, request.subject(), request.score(), request.semester()
        );
        return AcademicDtos.AcademicRecordResponse.from(academicRepository.save(record));
    }

    @Transactional
    public AcademicDtos.AcademicRecordResponse updateRecord(
            String email,
            Long id,
            AcademicDtos.AcademicRecordRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        AcademicRecord record = requireRecord(id, student);
        record.update(request.subject(), request.score(), request.semester());
        return AcademicDtos.AcademicRecordResponse.from(record);
    }

    @Transactional
    public void deleteRecord(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        AcademicRecord record = requireRecord(id, student);
        academicRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.StudyPlanResponse> getStudyPlans(String email) {
        Student student = studentService.requireCurrentStudent(email);
        return studyPlanRepository.findAllByStudentOrderBySubjectAsc(student)
                .stream().map(AcademicDtos.StudyPlanResponse::from).toList();
    }

    @Transactional
    public AcademicDtos.StudyPlanResponse createStudyPlan(
            String email,
            AcademicDtos.StudyPlanRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        StudyPlan plan = new StudyPlan(
                student, request.subject(), request.targetHours(), request.completedHours()
        );
        return AcademicDtos.StudyPlanResponse.from(studyPlanRepository.save(plan));
    }

    @Transactional
    public AcademicDtos.StudyPlanResponse updateStudyPlan(
            String email,
            Long id,
            AcademicDtos.StudyPlanRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        StudyPlan plan = requireStudyPlan(id, student);
        plan.update(request.subject(), request.targetHours(), request.completedHours());
        return AcademicDtos.StudyPlanResponse.from(plan);
    }

    @Transactional
    public AcademicDtos.StudyPlanResponse updateStudyPlanProgress(
            String email,
            Long id,
            AcademicDtos.ProgressRequest request
    ) {
        Student student = studentService.requireCurrentStudent(email);
        StudyPlan plan = requireStudyPlan(id, student);
        plan.updateProgress(request.completedHours());
        return AcademicDtos.StudyPlanResponse.from(plan);
    }

    @Transactional
    public void deleteStudyPlan(String email, Long id) {
        Student student = studentService.requireCurrentStudent(email);
        StudyPlan plan = requireStudyPlan(id, student);
        studyPlanRepository.delete(plan);
    }

    private AcademicRecord requireRecord(Long id, Student student) {
        return academicRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Academic record not found"));
    }

    private StudyPlan requireStudyPlan(Long id, Student student) {
        return studyPlanRepository.findByIdAndStudent(id, student)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Study plan not found"));
    }
}
