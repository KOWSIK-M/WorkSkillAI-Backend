package com.cp.workskillai.service;

import com.cp.workskillai.dto.SignUpRequest;
import com.cp.workskillai.dto.SignUpResponse;
import com.cp.workskillai.models.Student;
import com.cp.workskillai.dto.LoginRequest;
import com.cp.workskillai.dto.LoginResponse;
public interface StudentService {
    SignUpResponse registerStudent(SignUpRequest signUpRequest);
    boolean verifyEmail(String token);
    boolean resendVerificationEmail(String email);
    boolean initiatePasswordReset(String email);
    boolean resetPassword(String token, String newPassword);
    Student getStudentProfile(String studentId);
    Student updateStudentProfile(String studentId, Student student);
    LoginResponse loginStudent(LoginRequest loginRequest);
    Student getStudentById(String id);
}