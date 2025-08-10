package com.example.gitsync.service;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import com.example.gitsync.factory.CustomSshSessionFactory; // <-- 匯入我們自訂的 Factory
import com.example.gitsync.model.GitCommit;
import com.example.gitsync.model.WorkLog;
import com.example.gitsync.repository.WorkLogRepository;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitService {
    private final WorkLogRepository workLogRepository;
    private final WorkHourCalculator workHourCalculator;

    public GitService(WorkLogRepository workLogRepository, WorkHourCalculator workHourCalculator) {
        this.workLogRepository = workLogRepository;
        this.workHourCalculator = workHourCalculator;
    }

    public List<GitCommit> fetchCommits(String repoUrl, String username, String password) throws Exception {
        if (repoUrl.startsWith("ssh://")) {
            // <-- 變更：將帳號密碼傳遞給 SSH 方法
            return fetchCommitsViaSsh(repoUrl, username, password);
        } else {
            return fetchCommitsViaHttp(repoUrl, username, password);
        }
    }

private List<GitCommit> fetchCommitsViaHttp(String repoUrl, String username, String password) throws Exception {
        List<GitCommit> commits = new ArrayList<>();
        File localPath = File.createTempFile("TempGitRepo", "");
        localPath.delete();

        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                    .setDirectory(localPath)
                    .call();

            Iterable<RevCommit> log = git.log().call();
            for (RevCommit commit : log) {
                GitCommit gitCommit = new GitCommit();
                gitCommit.setCommitId(commit.getName());
                gitCommit.setAuthor(commit.getAuthorIdent().getName());
                gitCommit.setMessage(commit.getFullMessage());
                gitCommit.setCommitDate(commit.getAuthorIdent().getWhen().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime());
                commits.add(gitCommit);
            }
        } finally {
            if (git != null) {
                git.close();
            }
        }
        return commits;
    }

    /**
     * 更新後的 SSH 連線方法，使用帳號密碼
     */
    private List<GitCommit> fetchCommitsViaSsh(String repoUrl, String username, String password) throws Exception {
        System.out.println("Fetching commits via SSH...");
        System.out.println(username);
        System.out.println(password);
        List<GitCommit> commits = new ArrayList<>();
        // 建立暫存目錄來存放 clone 下來的專案
        File localPath = Files.createTempDirectory("TempGitRepo").toFile();

        // <-- 1. 建立我們自訂的 SSH Session Factory
        CustomSshSessionFactory sshSessionFactory = new CustomSshSessionFactory();

        // <-- 2. 設定 JGit 全域使用這個 Factory 實例
        // 這是一個靜態方法，會影響當前 JVM 中所有的 JGit SSH 操作
        SshSessionFactory.setInstance(sshSessionFactory);

        // <-- 3. 建立帳號密碼的 CredentialsProvider
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);

        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localPath)
                    
                    // <-- 4. 將 CredentialsProvider 設定給 clone 指令
                    // JGit 的 SSH 實作會自動使用它來進行密碼驗證
                    .setCredentialsProvider(credentialsProvider)
                    // <-- 5. 不再需要 setTransportConfigCallback，因為我們用了全域設定
                    .call();

            Iterable<RevCommit> log = git.log().call();
            for (RevCommit commit : log) {
                GitCommit gitCommit = new GitCommit();
                gitCommit.setCommitId(commit.getName());
                gitCommit.setAuthor(commit.getAuthorIdent().getName());
                gitCommit.setMessage(commit.getFullMessage());
                gitCommit.setCommitDate(commit.getAuthorIdent().getWhen().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime());
                commits.add(gitCommit);
            }
        } finally {
            if (git != null) {
                git.close();
            }
            // 刪除暫存目錄
            // (可加入更完善的遞迴刪除邏輯)
            if (localPath.exists()) {
                //簡易刪除
                localPath.delete();
            }
        }
        return commits;
    }

    /**
     * 從 Git 倉庫獲取所有 commits，並處理驗證
     * @param repoUrl 倉庫 URL
     * @param username 使用者名稱 (可為 null)
     * @param password 密碼 (可為 null)
     * @return RevCommit 列表
     */
    public List<RevCommit> fetchAllCommits(String repoUrl, String username, String password) throws Exception {
        File localPath = Files.createTempDirectory("TempGitRepo").toFile();
        List<RevCommit> commitList = new ArrayList<>();

        // 建立 clone 指令
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                
                .setDirectory(localPath);

        // 根據 URL 類型設定驗證
        if (repoUrl.startsWith("ssh://")) {
            // --- SSH 驗證邏輯 (新方法) ---

            // 1. 建立我們的自訂 SSH Factory 實例
            CustomSshSessionFactory sshSessionFactory = new CustomSshSessionFactory();
            
            // 2. 使用 TransportConfigCallback 將 Factory 應用到這次操作上
            cloneCommand.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport) {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });

            // 3. 如果提供了帳號密碼，設定 CredentialsProvider
            if (username != null && password != null) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }
        } else {
            // --- HTTP/S 驗證邏輯 ---
            if (username != null && password != null) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }
        }
        
        // 執行 clone 並處理 commit
        try (Git git = cloneCommand.call()) {
            Iterable<RevCommit> logs = git.log().all().call();
            logs.forEach(commitList::add);
        } finally {
            if (localPath.exists()) {
                // 簡易刪除
                localPath.delete();
            }
        }
        return commitList;
    }
    /**
     * 同步 Git Log 到資料庫
     * @param commits 從 Git 獲取的 commit 列表
     */
    public void syncCommitsToDatabase(List<RevCommit> commits) {
        List<WorkLog> workLogs = new ArrayList<>();
        for (RevCommit commit : commits) {
            WorkLog workLog = new WorkLog();
            LocalDateTime commitDateTime = Instant.ofEpochSecond(commit.getCommitTime())
                                                  .atZone(ZoneId.systemDefault())
                                                  .toLocalDateTime();
            
            workLog.setCommitHash(commit.getName());
            workLog.setAuthorName(commit.getAuthorIdent().getName());
            workLog.setMessage(commit.getShortMessage());
            workLog.setCommitDateTime(commitDateTime);
            
            // 使用 Calculator 計算工時
            double hours = workHourCalculator.calculateHours(commitDateTime);
            workLog.setWorkingHours(hours);
            
            workLogs.add(workLog);
        }
        
        if (!workLogs.isEmpty()) {
            workLogRepository.saveAll(workLogs);
        }
    }
}