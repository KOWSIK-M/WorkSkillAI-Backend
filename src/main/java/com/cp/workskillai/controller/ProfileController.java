package com.cp.workskillai.controller;

import com.cp.workskillai.dto.ProfileUpdateRequest;
import com.cp.workskillai.dto.ResumeAnalysisResponse;
import com.cp.workskillai.models.ResumeDocument;
import com.cp.workskillai.models.UserProfile;
import com.cp.workskillai.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String userId) {
        try {
            UserProfile profile = profileService.getProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching profile for user: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfile> updateProfile(
            @PathVariable String userId,
            @RequestBody ProfileUpdateRequest request) {
        try {
            UserProfile updatedProfile = profileService.updateProfile(userId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{userId}/resume/upload")
    public ResponseEntity<ResumeAnalysisResponse> uploadResume(
            @PathVariable String userId,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Received resume upload for user: {}, file: {}", userId, file.getOriginalFilename());
            ResumeAnalysisResponse response = profileService.uploadAndAnalyzeResume(userId, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading resume for user: {}", userId, e);
            return ResponseEntity.badRequest().body(
                new ResumeAnalysisResponse(false, e.getMessage(), null, null, null,null)
            );
        }
    }

    @GetMapping("/{userId}/resumes")
    public ResponseEntity<List<ResumeDocument>> getUserResumes(@PathVariable String userId) {
        try {
            List<ResumeDocument> resumes = profileService.getUserResumes(userId);
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            log.error("Error fetching resumes for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{userId}/resumes/history")
    public ResponseEntity<List<ResumeDocument>> getResumeHistory(@PathVariable String userId) {
        try {
            List<ResumeDocument> history = profileService.getResumeHistory(userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching resume history for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{userId}/resumes/{resumeId}")
    public ResponseEntity<String> deleteResume(
            @PathVariable String userId,
            @PathVariable String resumeId) {
        try {
            boolean deleted = profileService.deleteResume(resumeId, userId);
            return ResponseEntity.ok("Resume deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting resume: {} for user: {}", resumeId, userId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{userId}/resumes/{resumeId}/active")
    public ResponseEntity<String> setActiveResume(
            @PathVariable String userId,
            @PathVariable String resumeId) {
        try {
            boolean updated = profileService.setActiveResume(resumeId, userId);
            return ResponseEntity.ok("Resume set as active");
        } catch (Exception e) {
            log.error("Error setting active resume: {} for user: {}", resumeId, userId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resumes/{resumeId}/reanalyze")
    public ResponseEntity<ResumeAnalysisResponse> reanalyzeResume(@PathVariable String resumeId) {
        try {
            ResumeAnalysisResponse response = profileService.analyzeExistingResume(resumeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reanalyzing resume: {}", resumeId, e);
            return ResponseEntity.badRequest().body(
                new ResumeAnalysisResponse(false, e.getMessage(), null, null, null,null)
            );
        }
    }
}