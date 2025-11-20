// SkillGapAnalysisRepository.java
package com.cp.workskillai.repository;

import com.cp.workskillai.models.SkillGapAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGapAnalysisRepository extends MongoRepository<SkillGapAnalysis, String> {
    
    List<SkillGapAnalysis> findByUserIdOrderByAnalyzedAtDesc(String userId);
    
    Optional<SkillGapAnalysis> findByUserIdAndIsCurrentRoleTrue(String userId);
    
    List<SkillGapAnalysis> findByUserIdAndJobRoleOrderByAnalyzedAtDesc(String userId, String jobRole);
    
    @Query("{ 'user_id': ?0 }")
    @Update("{ '$set': { 'is_current_role': false } }")
    void clearCurrentRoleFlag(String userId);
    
    @Query("{ 'user_id': ?0, 'is_current_role': true }")
    Optional<SkillGapAnalysis> findCurrentRoleAnalysis(String userId);
    
 // Find all analyses for a specific user
    List<SkillGapAnalysis> findByUserId(String userId);
    
    // Find the latest analysis for a user
    Optional<SkillGapAnalysis> findTopByUserIdOrderByAnalyzedAtDesc(String userId);
    
    // Find analyses by user ID and job role
    List<SkillGapAnalysis> findByUserIdAndJobRole(String userId, String jobRole);
    
    // Find the latest analysis for a specific job role
    Optional<SkillGapAnalysis> findTopByUserIdAndJobRoleOrderByAnalyzedAtDesc(String userId, String jobRole);
    
    // Find analyses where this is marked as current role
    List<SkillGapAnalysis> findByUserIdAndIsCurrentRole(String userId, Boolean isCurrentRole);
    
    // Find analyses with match score greater than threshold
    List<SkillGapAnalysis> findByUserIdAndMatchScoreGreaterThan(String userId, Double minScore);
    
    // Find analyses with match score less than threshold
    List<SkillGapAnalysis> findByUserIdAndMatchScoreLessThan(String userId, Double maxScore);
    
    // Count analyses for a user
    Long countByUserId(String userId);
    
    // Delete all analyses for a user
    void deleteByUserId(String userId);
    
    // Find analyses by multiple user IDs (useful for HR views)
    List<SkillGapAnalysis> findByUserIdIn(List<String> userIds);
    
    // Custom query to find analyses with specific skills in missing skills
    @Query("{ 'userId': ?0, 'missingSkills': { $elemMatch: { 'name': ?1 } } }")
    List<SkillGapAnalysis> findByUserIdAndMissingSkillName(String userId, String skillName);
    
    // Custom query to find analyses with high importance missing skills
    @Query("{ 'userId': ?0, 'missingSkills': { $elemMatch: { 'importance': { $gt: ?1 } } } }")
    List<SkillGapAnalysis> findByUserIdAndHighImportanceMissingSkills(String userId, Double minImportance);
    
    // Custom query to get analysis history with pagination
    @Query(value = "{ 'userId': ?0 }", sort = "{ 'analyzedAt': -1 }")
    List<SkillGapAnalysis> findAnalysisHistoryByUserId(String userId);
    
    // Check if user has any analysis
    Boolean existsByUserId(String userId);
    
    // Find analyses by job role pattern (case-insensitive)
    @Query("{ 'userId': ?0, 'jobRole': { $regex: ?1, $options: 'i' } }")
    List<SkillGapAnalysis> findByUserIdAndJobRoleLike(String userId, String jobRolePattern);
    
    // Get distinct job roles analyzed for a user
    @Query(value = "{ 'userId': ?0 }", fields = "{ 'jobRole': 1 }")
    List<SkillGapAnalysis> findDistinctJobRolesByUserId(String userId);
    
    // Custom projection to get only analysis metadata (without heavy skill arrays)
    @Query(value = "{ 'userId': ?0 }", fields = "{ 'jobRole': 1, 'matchScore': 1, 'analyzedAt': 1, 'isCurrentRole': 1 }")
    List<SkillGapAnalysis> findAnalysisMetadataByUserId(String userId);
}