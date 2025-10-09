package com.cp.workskillai.controller;

import com.cp.workskillai.dto.SignUpRequest;
import com.cp.workskillai.dto.SignUpResponse;
import com.cp.workskillai.service.StudentService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cp.workskillai.config.JwtCookieService;
import com.cp.workskillai.dto.LoginRequest;
import com.cp.workskillai.dto.LoginResponse;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class AuthController {

    private final StudentService studentService;
    private final JwtCookieService jwtCookieService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> registerStudent(
            @Valid @RequestBody SignUpRequest signUpRequest) {
        
        log.info("Received signup request for email: {}", signUpRequest.getEmail());
        
        try {
            SignUpResponse response = studentService.registerStudent(signUpRequest);
            
            if (response.isSuccess()) {
                log.info("Signup successful for email: {}", signUpRequest.getEmail());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Signup failed for email: {} - {}", signUpRequest.getEmail(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Server error during signup for email: {}", signUpRequest.getEmail(), e);
            return ResponseEntity.internalServerError().body(
                new SignUpResponse(false, "Internal server error", null, null, null)
            );
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginStudent(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        
        log.info("Received login request for email: {}", loginRequest.getEmail());
        
        try {
            LoginResponse loginResponse = studentService.loginStudent(loginRequest);
            
            if (loginResponse.isSuccess()) {
                // Create HTTP-only cookie
                ResponseCookie jwtCookie = jwtCookieService.createJwtCookie(loginResponse.getToken());
                response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
                
                log.info("Login successful for email: {}", loginRequest.getEmail());
                return ResponseEntity.ok(loginResponse);
            } else {
                log.warn("Login failed for email: {} - {}", loginRequest.getEmail(), loginResponse.getMessage());
                return ResponseEntity.badRequest().body(loginResponse);
            }
        } catch (Exception e) {
            log.error("Server error during login for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.internalServerError().body(
                new LoginResponse(false, "Internal server error", null, null, null, null, null, null)
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        try {
            // Clear the JWT cookie
            ResponseCookie logoutCookie = jwtCookieService.createLogoutCookie();
            response.addHeader(HttpHeaders.SET_COOKIE, logoutCookie.toString());
            
            log.info("User logged out successfully");
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.internalServerError().body("Logout failed");
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken() {
        // The token is automatically validated by Spring Security filters
        // This endpoint just confirms the token is valid
        return ResponseEntity.ok(true);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        log.info("Received email verification request for token: {}", token);
        
        try {
            boolean isVerified = studentService.verifyEmail(token);
            
            if (isVerified) {
                return ResponseEntity.ok("Email verified successfully");
            } else {
                return ResponseEntity.badRequest().body("Invalid or expired verification token");
            }
        } catch (Exception e) {
            log.error("Error during email verification for token: {}", token, e);
            return ResponseEntity.internalServerError().body("Internal server error during email verification");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam String email) {
        log.info("Received resend verification request for email: {}", email);
        
        try {
            boolean isSent = studentService.resendVerificationEmail(email);
            
            if (isSent) {
                return ResponseEntity.ok("Verification email sent successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to send verification email");
            }
        } catch (Exception e) {
            log.error("Error during resend verification for email: {}", email, e);
            return ResponseEntity.internalServerError().body("Internal server error");
        }
    }

    // Simple health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Auth service is running");
    }
}