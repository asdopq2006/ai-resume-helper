package com.example.airesumehelper;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class AnalysisHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String resumeText;
    private String skills;      // 存储 JSON 字符串，如 "Java,Spring Boot"
    private Integer yearsOfExperience;
    private LocalDateTime createTime;
    private String education;
    private String expectedPosition;

    // 全参构造、getter/setter（使用 Lombok 或手写）
    // 为了节省时间，推荐用 Lombok：在类上加 @Data @NoArgsConstructor @AllArgsConstructor
}
