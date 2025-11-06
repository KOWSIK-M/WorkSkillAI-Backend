// RecommendationController.java
package com.cp.workskillai.controller;

import com.cp.workskillai.models.RecommendationResponse;
import com.cp.workskillai.service.RecommendationService;
import com.cp.workskillai.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserRecommendations(@PathVariable String userId) {
        try {
            RecommendationResponse recommendations = recommendationService.generateRecommendations(userId);
            return ResponseEntity.ok(ApiResponse.success(
                "Recommendations generated successfully", 
                recommendations
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to generate recommendations: " + e.getMessage())
            );
        }
    }
    
    @PostMapping("/save-enrollment")
    public ResponseEntity<ApiResponse> saveEnrollment(
            @RequestParam String userId,
            @RequestParam String courseId,
            @RequestParam String courseTitle) {
        try {
            // Save enrollment to database (you can create an Enrollment model)
            log.info("User {} enrolled in course: {}", userId, courseTitle);
            return ResponseEntity.ok(ApiResponse.success("Enrollment saved successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to save enrollment: " + e.getMessage())
            );
        }
    }
    
    @PostMapping("/save-course")
    public ResponseEntity<ApiResponse> saveCourseForLater(
            @RequestParam String userId,
            @RequestParam String courseId,
            @RequestParam String courseTitle) {
        try {
            // Save to saved courses in database
            log.info("User {} saved course for later: {}", userId, courseTitle);
            return ResponseEntity.ok(ApiResponse.success("Course saved successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to save course: " + e.getMessage())
            );
        }
    }
}