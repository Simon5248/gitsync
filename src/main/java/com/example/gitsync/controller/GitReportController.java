package com.example.gitsync.controller;

import com.example.gitsync.service.GitService;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GitReportController {
    @Autowired
    private GitService gitService;

    @GetMapping("/git-report")
    public String showForm() {
        return "git_report_form";
    }

    @PostMapping("/git-report/download")
    public ResponseEntity<InputStreamResource> downloadExcel(
            @RequestParam String repoUrl,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime effdate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expdate,
            @RequestParam(name = "branches", required = false) List<String> branches
    ) throws Exception {
        Map<String, List<RevCommit>> allData = gitService.fetchCommitsByDateRange(repoUrl, username, password, effdate, expdate, branches);
        ByteArrayOutputStream out = com.example.gitsync.util.ExcelReportUtil.generateExcel(allData);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(out.toByteArray()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=git_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
