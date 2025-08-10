package com.example.gitsync.model;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "WorkLog")
@Data
public class WorkLog {

    @Id
    @Column(name = "CommitHash", length = 40)
    private String commitHash;

    @Column(name = "AuthorName", length = 100)
    private String authorName;

    @Column(name = "CommitDateTime")
    private LocalDateTime commitDateTime;

    @Column(name = "Message", columnDefinition = "NVARCHAR(MAX)")
    private String message;

    // 新增欄位：用來記錄計算出的工時
    @Column(name = "WorkingHours")
    private double workingHours;
}