package com.example.gitsync.controller;

import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.gitsync.service.GitService;
import com.example.gitsync.service.HtmlReportGenerator;
import com.example.gitsync.repository.GitCommitRepository;
import com.example.gitsync.model.GitCommit;
import java.util.List;

@RestController
@RequestMapping("/api/git")
public class GitController {    
    private final GitService gitService;
    private final HtmlReportGenerator htmlReportGenerator;
    private final GitCommitRepository gitCommitRepository;

    public GitController(GitService gitService, HtmlReportGenerator htmlReportGenerator,GitCommitRepository gitCommitRepository) {
        this.gitService = gitService;
        this.htmlReportGenerator = htmlReportGenerator;
        this.gitCommitRepository = gitCommitRepository;
    }


    @PostMapping("/sync")
    public ResponseEntity<String> syncGitCommits(
            @RequestParam String repoUrl,
            @RequestParam String username,
            @RequestParam String password,
                                                     @RequestParam String branch) {
        try {
            List<GitCommit> commits = gitService.fetchCommits(repoUrl, username, password);
            gitCommitRepository.saveAll(commits);
            List<RevCommit> rcommits = gitService.fetchAllCommits(repoUrl, username, password, branch);
            gitService.syncCommitsToDatabase(rcommits);
            return ResponseEntity.ok("Git commits synced successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing Git commits: " + e.getMessage());
        }
    }

    @GetMapping(value = "/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getWorkHourReport(@RequestParam String repoUrl,
                                                     @RequestParam String username,
                                                     @RequestParam String password,
                                                     @RequestParam String branch
                                                     ) {
        try {
            System.out.println(repoUrl);
            System.out.println(username);
            System.out.println(password);
            List<RevCommit> commits = gitService.fetchAllCommits(repoUrl, username, password, branch);
            String reportHtml = htmlReportGenerator.generate(commits);
            return ResponseEntity.ok(reportHtml);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("<h1>產生報告失敗</h1><p>" + e.getMessage() + "</p>");
        }
    }
}