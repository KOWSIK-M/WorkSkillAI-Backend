package com.cp.workskillai.controller;

import com.cp.workskillai.models.UserSkill;
import com.cp.workskillai.service.GeminiExamService;
import com.cp.workskillai.service.UserSkillService;
import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class ExamController {
    
    private final GeminiExamService geminiExamService;
    private final UserSkillService userSkillService;
    
    @PostMapping("/generate-exam")
    public ResponseEntity<Map<String, Object>> generateExam(
            @RequestBody Map<String, Object> request) {
        
        try {
            String skill = (String) request.get("skill");
            String category = (String) request.get("category");
            String difficulty = (String) request.get("difficulty");
            Integer numberOfQuestions = (Integer) request.get("numberOfQuestions");
            
            if (skill == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Skill name is required"));
            }
            
            Map<String, Object> exam = geminiExamService.generateExamQuestions(
                skill, 
                category != null ? category : "General", 
                difficulty != null ? difficulty : "intermediate", 
                numberOfQuestions != null ? numberOfQuestions : 5
            );
            
            return ResponseEntity.ok(exam);
        } catch (Exception e) {
            log.error("Error generating exam", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate exam: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{skillId}/exam-result")
    public ResponseEntity<UserSkill> updateExamResult(
            @PathVariable String skillId,
            @RequestBody Map<String, Object> examResult) {
        try {
            Integer score = (Integer) examResult.get("score");
            String status = (String) examResult.get("status");
            
            UserSkill updatedSkill = userSkillService.updateSkillProficiency(skillId, score, status);
            if (updatedSkill != null) {
                return ResponseEntity.ok(updatedSkill);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating exam result for skill: {}", skillId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/evaluate-answer")
    public ResponseEntity<Map<String, Object>> evaluateAnswer(
            @RequestBody Map<String, Object> request) {
        
        String question = (String) request.get("question");
        String userAnswer = (String) request.get("userAnswer");
        String context = (String) request.get("context");
        
        Map<String, Object> evaluation = geminiExamService.evaluateAnswerWithAI(
            question, userAnswer, context
        );
        
        return ResponseEntity.ok(evaluation);
    }
}