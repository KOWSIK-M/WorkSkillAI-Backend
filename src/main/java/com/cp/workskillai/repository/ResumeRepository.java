package com.cp.workskillai.repository;

import com.cp.workskillai.models.ResumeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends MongoRepository<ResumeDocument, String> {
    List<ResumeDocument> findByUserIdOrderByUploadDateDesc(String userId);
    List<ResumeDocument> findTop4ByUserIdOrderByUploadDateDesc(String userId);
    Optional<ResumeDocument> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
    Optional<ResumeDocument> findByUserIdAndIsActiveTrue(String userId);
    List<ResumeDocument> findByUserId(String userId);
    
    @Query(value = "{'userId': ?0}", sort = "{'uploadDate': -1}")
    List<ResumeDocument> findRecentResumesByUserId(String userId);
}