package com.example.airesumehelper

import org.springframework.data.jpa.repository.JpaRepository

interface AnalysisHistoryRepository  extends JpaRepository<AnalysisHistory,Long>{
    List<AnalysisHistory> findAllByOrderByCreateTimeDesc();
}