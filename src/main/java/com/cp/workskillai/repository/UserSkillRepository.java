package com.cp.workskillai.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cp.workskillai.models.UserSkill;

public interface UserSkillRepository extends MongoRepository<UserSkill, String>{
	List<UserSkill> findByUserId(String userId);
	List<UserSkill> findByUserIdAndVerified(String userId, Boolean verified);
    boolean existsByUserIdAndName(String userId, String name);
}
