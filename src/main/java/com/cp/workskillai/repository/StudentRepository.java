package com.cp.workskillai.repository;

import com.cp.workskillai.models.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends MongoRepository<Student, String> {
    
    Optional<Student> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("{ 'email' : ?0, 'isActive' : true }")
    Optional<Student> findByEmailAndActive(String email);
    
    Optional<Student> findByVerificationToken(String verificationToken);
    
    Optional<Student> findByResetPasswordToken(String resetPasswordToken);
}