package com.example.airesumehelper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    List<AnalysisHistory> findAllByOrderByCreateTimeDesc();

}
