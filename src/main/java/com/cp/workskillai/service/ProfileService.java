package com.cp.workskillai.service;

import com.cp.workskillai.dto.ProfileUpdateRequest;
import com.cp.workskillai.dto.ResumeAnalysisResponse;
import com.cp.workskillai.dto.ResumeUploadRequest;
import com.cp.workskillai.models.ResumeDocument;
import com.cp.workskillai.models.UserProfile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfileService {
    
    // Profile Management
    UserProfile getProfile(String userId);
    UserProfile updateProfile(String userId, ProfileUpdateRequest request);
    UserProfile createProfile(String userId, ProfileUpdateRequest request);
    
    // Resume Management
    ResumeAnalysisResponse uploadAndAnalyzeResume(String userId, MultipartFile file);
    List<ResumeDocument> getUserResumes(String userId);
    ResumeDocument getResumeById(String resumeId);
    boolean deleteResume(String resumeId, String userId);
    boolean setActiveResume(String resumeId, String userId);
    
    // Resume History (last 4)
    List<ResumeDocument> getResumeHistory(String userId);
    
    // Analysis
    ResumeAnalysisResponse analyzeExistingResume(String resumeId);
}