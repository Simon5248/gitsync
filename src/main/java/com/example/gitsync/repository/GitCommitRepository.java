package com.example.gitsync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gitsync.model.GitCommit;

public interface GitCommitRepository extends JpaRepository<GitCommit, Long> {
}