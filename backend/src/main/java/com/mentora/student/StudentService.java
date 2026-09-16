package com.mentora.student;

import com.mentora.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public StudentDtos.Profile getCurrentProfile(String authenticatedEmail) {
        return StudentDtos.Profile.from(requireCurrentStudent(authenticatedEmail));
    }

    @Transactional
    public StudentDtos.Profile updateCurrentProfile(
            String authenticatedEmail,
            StudentDtos.UpdateRequest request
    ) {
        Student student = requireCurrentStudent(authenticatedEmail);
        student.updateProfile(request.name(), request.branch(), request.semester());
        return StudentDtos.Profile.from(student);
    }

    @Transactional(readOnly = true)
    public Student requireCurrentStudent(String authenticatedEmail) {
        return studentRepository.findByUserEmail(authenticatedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student profile not found"));
    }
}
