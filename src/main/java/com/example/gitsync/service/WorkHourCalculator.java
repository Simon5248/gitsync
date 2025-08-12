package com.example.gitsync.service;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
public class WorkHourCalculator {

    // 定義每日的四個工作開始時間點
    private static final List<LocalTime> START_TIMES = Arrays.asList(
            LocalTime.of(6, 0),
            LocalTime.of(14, 0),
            LocalTime.of(18, 0),
            LocalTime.of(20, 0)
    );

    /**
     * 根據 commit 時間計算工時
     * @param commitDateTime commit 的時間點
     * @return 計算出的小時數 (例如 4.5)
     */
    public double calculateHours(LocalDateTime commitDateTime) {
        LocalDate commitDate = commitDateTime.toLocalDate();
        LocalTime commitTime = commitDateTime.toLocalTime();

        LocalDateTime nearestStartDateTime = findNearestStartDateTime(commitDateTime);

        // 使用 Duration 計算兩個時間點之間的差距
        Duration duration = Duration.between(nearestStartDateTime, commitDateTime);

        // 將差距轉換為小時 (帶小數點)
        return duration.toMinutes() / 60.0;
    }

    /**
     * 找到離 commit 時間最近的一個過去的開始時間點
     */
    private LocalDateTime findNearestStartDateTime(LocalDateTime commitDateTime) {
        LocalTime commitTime = commitDateTime.toLocalTime();

        // 從最後一個開始時間 (20:00) 往前找
        for (int i = START_TIMES.size() - 1; i >= 0; i--) {
            LocalTime startTime = START_TIMES.get(i);
            if (!commitTime.isBefore(startTime)) {
                // 如果 commit 時間在某個開始時間之後或等於，那就是這個開始時間
                return LocalDateTime.of(commitDateTime.toLocalDate(), startTime);
            }
        }

        // 如果執行到這裡，表示 commit 時間在 00:00 到 06:00 之間
        // 這種情況下，開始時間是前一天的最後一個時段 (20:00)
        return LocalDateTime.of(commitDateTime.toLocalDate().minusDays(1), START_TIMES.get(START_TIMES.size() - 1));
    }
    
    /**
     * 根據 commit 時間找到它屬於哪個時間區段
     */
    public LocalTime getTimeSlot(LocalDateTime commitDateTime) {
        LocalTime commitTime = commitDateTime.toLocalTime();
        for (int i = START_TIMES.size() - 1; i >= 0; i--) {
            LocalTime startTime = START_TIMES.get(i);
            if (!commitTime.isBefore(startTime)) {
                return startTime;
            }
        }
        // 屬於前一天 20:00 的區段
        return START_TIMES.get(START_TIMES.size() - 1);
    }
}
