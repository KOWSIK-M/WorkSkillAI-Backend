package com.cp.workskillai.repository;

import com.cp.workskillai.models.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
    
    @Query("{'userId': ?0}")
    Optional<UserProfile> findProfileByUserId(String userId);
}