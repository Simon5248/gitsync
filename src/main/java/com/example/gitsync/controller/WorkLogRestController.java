package com.example.gitsync.controller;

import com.example.gitsync.model.WorkLog;
import com.example.gitsync.repository.WorkLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
public class WorkLogRestController {
    @Autowired
    private WorkLogRepository workLogRepository;

    @GetMapping("/api/worklog/search")
    public Map<String, Map<String, Map<LocalDate, List<WorkLog>>>> search(
            @RequestParam(required = false) String commitHash,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) Double workingHours,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        List<WorkLog> logs = workLogRepository.findAll();
        if (commitHash != null && !commitHash.isEmpty()) {
            logs = logs.stream().filter(l -> l.getCommitHash() != null && l.getCommitHash().contains(commitHash)).collect(Collectors.toList());
        }
        if (authorName != null && !authorName.isEmpty()) {
            logs = logs.stream().filter(l -> l.getAuthorName() != null && l.getAuthorName().contains(authorName)).collect(Collectors.toList());
        }
        if (branchName != null && !branchName.isEmpty()) {
            logs = logs.stream().filter(l -> l.getBranchName() != null && l.getBranchName().contains(branchName)).collect(Collectors.toList());
        }
        if (message != null && !message.isEmpty()) {
            logs = logs.stream().filter(l -> l.getMessage() != null && l.getMessage().contains(message)).collect(Collectors.toList());
        }
        if (workingHours != null) {
            logs = logs.stream().filter(l -> l.getWorkingHours() == workingHours).collect(Collectors.toList());
        }
        if (dateFrom != null) {
            logs = logs.stream().filter(l -> l.getCommitDateTime() != null && !l.getCommitDateTime().toLocalDate().isBefore(dateFrom)).collect(Collectors.toList());
        }
        if (dateTo != null) {
            logs = logs.stream().filter(l -> l.getCommitDateTime() != null && !l.getCommitDateTime().toLocalDate().isAfter(dateTo)).collect(Collectors.toList());
        }
        // 分組：分支+作者+日期
        Map<String, Map<String, Map<LocalDate, List<WorkLog>>>> groupMap = new HashMap<>();
        for (WorkLog log : logs) {
            String branch = log.getBranchName();
            String author = log.getAuthorName();
            LocalDate date = log.getCommitDateTime().toLocalDate();
            groupMap.computeIfAbsent(branch, k -> new HashMap<>())
                    .computeIfAbsent(author, k -> new HashMap<>())
                    .computeIfAbsent(date, k -> new java.util.ArrayList<>())
                    .add(log);
        }
        return groupMap;
    }
}
