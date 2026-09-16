package com.mentora.student;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentora.auth.User;
import com.mentora.common.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String branch;

    @Column(nullable = false)
    private int semester;

    protected Student() {
    }

    public Student(User user, String name, String branch, int semester) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is required");
        }
        this.user = user;
        updateProfile(name, branch, semester);
    }

    public void updateProfile(String name, String branch, int semester) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Student name is required");
        }
        if (branch == null || branch.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Branch is required");
        }
        String cleanName = name.trim();
        String cleanBranch = branch.trim();
        if (cleanName.length() > 100 || cleanBranch.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name and branch must not exceed 100 characters");
        }
        if (semester < 1 || semester > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Semester must be between 1 and 12");
        }
        this.name = cleanName;
        this.branch = cleanBranch;
        this.semester = semester;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getBranch() {
        return branch;
    }

    public int getSemester() {
        return semester;
    }
}
