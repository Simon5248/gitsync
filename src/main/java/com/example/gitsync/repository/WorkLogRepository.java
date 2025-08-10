package com.example.gitsync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gitsync.model.WorkLog;

public interface WorkLogRepository extends JpaRepository<WorkLog, String> {
}