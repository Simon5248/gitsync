package com.example.gitsync.service;

import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HtmlReportGenerator {

    private final WorkHourCalculator workHourCalculator;

    public HtmlReportGenerator(WorkHourCalculator workHourCalculator) {
        this.workHourCalculator = workHourCalculator;
    }

    public String generate(List<RevCommit> commits) {
        // 1. 處理資料：將 commits 按日期和時間段分組
        Map<LocalDate, Map<LocalTime, List<RevCommit>>> groupedByDayAndSlot = commits.stream()
            .collect(Collectors.groupingBy(
                commit -> LocalDateTime.ofInstant(commit.getAuthorIdent().getWhen().toInstant(), ZoneId.systemDefault()).toLocalDate(),
                TreeMap::new, // 按日期排序
                Collectors.groupingBy(
                    commit -> workHourCalculator.getTimeSlot(LocalDateTime.ofInstant(commit.getAuthorIdent().getWhen().toInstant(), ZoneId.systemDefault())),
                    TreeMap::new, // 按時間段排序
                    Collectors.toList()
                )
            ));

        // 2. 建立 HTML
        StringBuilder html = new StringBuilder();
        appendHtmlHeader(html);

        // 3. 遍歷處理好的資料來產生表格內容
        for (Map.Entry<LocalDate, Map<LocalTime, List<RevCommit>>> dayEntry : groupedByDayAndSlot.entrySet()) {
            LocalDate date = dayEntry.getKey();
            Map<LocalTime, List<RevCommit>> slots = dayEntry.getValue();
            double dailyTotalHours = 0;

            // 取得每個時間段的資料
            String slot06 = generateSlotHtml(slots.get(LocalTime.of(6, 0)));
            String slot14 = generateSlotHtml(slots.get(LocalTime.of(14, 0)));
            String slot18 = generateSlotHtml(slots.get(LocalTime.of(18, 0)));
            String slot22 = generateSlotHtml(slots.get(LocalTime.of(22, 0)));
            
            // 計算每日總工時
            dailyTotalHours += calculateSlotHours(slots.get(LocalTime.of(6, 0)));
            dailyTotalHours += calculateSlotHours(slots.get(LocalTime.of(14, 0)));
            dailyTotalHours += calculateSlotHours(slots.get(LocalTime.of(18, 0)));
            dailyTotalHours += calculateSlotHours(slots.get(LocalTime.of(22, 0)));

            // 產生一天的 row
            html.append("<tr>")
                .append("<td>").append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("</td>")
                .append("<td>").append(slot06).append("</td>")
                .append("<td>").append(slot14).append("</td>")
                .append("<td>").append(slot18).append("</td>")
                .append("<td>").append(slot22).append("</td>")
                .append("<td class='total-hours'>").append(String.format("%.2f", dailyTotalHours)).append(" 小時</td>")
                .append("</tr>");
        }

        appendHtmlFooter(html);
        return html.toString();
    }

    private double calculateSlotHours(List<RevCommit> slotCommits) {
        if (slotCommits == null || slotCommits.isEmpty()) {
            return 0;
        }
        // 找到這個時間段中最新的 commit
        RevCommit lastCommit = slotCommits.stream()
            .max(Comparator.comparing(RevCommit::getCommitTime))
            .orElse(null);
        
        if(lastCommit == null) return 0;

        LocalDateTime commitTime = LocalDateTime.ofInstant(lastCommit.getAuthorIdent().getWhen().toInstant(), ZoneId.systemDefault());
        return workHourCalculator.calculateHours(commitTime);
    }

    private String generateSlotHtml(List<RevCommit> slotCommits) {
        if (slotCommits == null || slotCommits.isEmpty()) {
            return "";
        }
        double hours = calculateSlotHours(slotCommits);
        
        StringBuilder content = new StringBuilder();
        content.append("<div class='hours'>工時: ").append(String.format("%.2f", hours)).append(" 小時</div>");
        content.append("<ul class='commit-list'>");
        for (RevCommit commit : slotCommits) {
            content.append("<li>").append(commit.getShortMessage()).append("</li>");
        }
        content.append("</ul>");
        return content.toString();
    }

    private void appendHtmlHeader(StringBuilder html) {
        html.append("<!DOCTYPE html><html lang='zh-Hant'><head><meta charset='UTF-8'><title>工時報告</title>");
        html.append("<style>")
            .append("body { font-family: 'Segoe UI', sans-serif; margin: 20px; background-color: #f4f7f6; }")
            .append("h1 { color: #333; }")
            .append("table { width: 100%; border-collapse: collapse; box-shadow: 0 2px 15px rgba(0,0,0,0.1); background-color: white; }")
            .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; vertical-align: top; }")
            .append("th { background-color: #007bff; color: white; }")
            .append("tr:nth-child(even) { background-color: #f2f2f2; }")
            .append(".hours { font-weight: bold; color: #0056b3; margin-bottom: 5px; }")
            .append(".commit-list { margin: 0; padding-left: 20px; color: #555; }")
            .append(".total-hours { font-weight: bold; background-color: #e9ecef; color: #d9534f; }")
            .append("</style></head><body><h1>Git 提交工時報告</h1><table>")
            .append("<tr><th>日期</th><th>06:00 時段</th><th>14:00 時段</th><th>18:00 時段</th><th>22:00 時段</th><th>當日總計</th></tr>");
    }

    private void appendHtmlFooter(StringBuilder html) {
        html.append("</table></body></html>");
    }
}