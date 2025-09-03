package com.example.gitsync.controller;

import com.example.gitsync.service.GitService;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class GitSyncController {
    @Autowired
    private GitService gitService;

    @PostMapping("/git-report/sync")
    public String syncToDb(
            @RequestParam String repoUrl,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime effdate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expdate,
            @RequestParam(name = "branches", required = false) List<String> branches,
            RedirectAttributes redirectAttributes
    ) throws Exception {
        // 取得 commit
        Map<String, List<RevCommit>> branchCommits = gitService.fetchCommitsByDateRange(repoUrl, username, password, effdate, expdate, branches);
        // 將 branch name 一併記錄到 WorkLog
        List<com.example.gitsync.model.WorkLog> workLogs = new java.util.ArrayList<>();
        for (Map.Entry<String, List<RevCommit>> entry : branchCommits.entrySet()) {
            String branchName = entry.getKey();
            for (RevCommit commit : entry.getValue()) {
                com.example.gitsync.model.WorkLog workLog = new com.example.gitsync.model.WorkLog();
                java.time.LocalDateTime commitDateTime = java.time.Instant.ofEpochSecond(commit.getCommitTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                workLog.setCommitHash(commit.getName());
                workLog.setAuthorName(commit.getAuthorIdent().getName().toUpperCase());
                workLog.setMessage(commit.getShortMessage().toUpperCase());
                workLog.setCommitDateTime(commitDateTime);
                workLog.setBranchName(branchName);
                workLog.setGitUrl(repoUrl); // 新增 gitUrl 欄位
                workLog.setUpdateDateTime(java.time.LocalDateTime.now()); // 新增 updateDateTime 欄位
                workLogs.add(workLog);
            }
        }
        // 寫入 DB
        if (!workLogs.isEmpty()) {
            gitService.syncWorkLogsToDatabase(workLogs);
        }
        redirectAttributes.addFlashAttribute("syncMsg", "同步完成，已寫入 " + workLogs.size() + " 筆 commit。");
        return "redirect:/git-report";
    }
}
