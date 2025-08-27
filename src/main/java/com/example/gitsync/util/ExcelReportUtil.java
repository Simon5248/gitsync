package com.example.gitsync.util;

import org.eclipse.jgit.revwalk.RevCommit;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReportUtil {
    public static ByteArrayOutputStream generateExcel(Map<String, List<RevCommit>> branchCommitsMap) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("CTBC 人天報告");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("分支");
        header.createCell(1).setCellValue("commit");
        header.createCell(2).setCellValue("作者");
        header.createCell(3).setCellValue("日期");
        header.createCell(4).setCellValue("人天");
        int rowIdx = 1;
        int totalDays = 0;
        // 分組：分支+作者+日期
        Map<String, Map<String, Map<java.time.LocalDate, List<RevCommit>>>> groupMap = new HashMap<>();
        for (Map.Entry<String, List<RevCommit>> entry : branchCommitsMap.entrySet()) {
            String branch = entry.getKey();
            for (RevCommit commit : entry.getValue()) {
                String author = commit.getAuthorIdent().getName();
                java.time.LocalDate date = java.time.LocalDateTime.ofInstant(commit.getAuthorIdent().getWhen().toInstant(), java.time.ZoneId.systemDefault()).toLocalDate();
                groupMap.computeIfAbsent(branch, k -> new HashMap<>())
                        .computeIfAbsent(author, k -> new HashMap<>())
                        .computeIfAbsent(date, k -> new ArrayList<>())
                        .add(commit);
            }
        }
        for (String branch : groupMap.keySet()) {
            Map<String, Map<java.time.LocalDate, List<RevCommit>>> authorMap = groupMap.get(branch);
            for (String author : authorMap.keySet()) {
                Map<java.time.LocalDate, List<RevCommit>> dateMap = authorMap.get(author);
                for (java.time.LocalDate date : dateMap.keySet()) {
                    List<RevCommit> commits = dateMap.get(date);
                    String commitStr = commits.stream()
                        .map(c -> c.getShortMessage() + " [" + c.getName().substring(0, 7) + "]")
                        .collect(java.util.stream.Collectors.joining("\n"));
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(branch);
                    row.createCell(1).setCellValue(commitStr);
                    row.createCell(2).setCellValue(author);
                    row.createCell(3).setCellValue(date.toString());
                    row.createCell(4).setCellValue(1);
                    totalDays++;
                }
            }
        }
        // 總人天
        Row totalRow = sheet.createRow(rowIdx);
        totalRow.createCell(0).setCellValue("總人天");
        totalRow.createCell(4).setCellValue(totalDays);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out;
    }
}
