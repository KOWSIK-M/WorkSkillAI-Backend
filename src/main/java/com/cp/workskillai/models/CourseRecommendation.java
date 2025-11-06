package com.cp.workskillai.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "course_recommendations")
public class CourseRecommendation {
    @Id
    private String id;
    
    private String userId;
    private String skillId;
    private String skillName;
    
    private String platform;
    private String title;
    private String instructor;
    private String description;
    private String url;
    
    private String duration;
    private String difficulty;
    private Double rating;
    private Integer studentCount;
    private String price;
    private String originalPrice;
    
    private List<String> features;
    private String durationCategory;
    private String platformIcon;
    private Double relevanceScore;
    
    private LocalDateTime createdAt;
}