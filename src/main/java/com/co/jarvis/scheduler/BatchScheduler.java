package com.co.jarvis.scheduler;

import com.co.jarvis.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BatchScheduler {

    private final BatchService batchService;

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Bogota")
    public void checkExpiredBatches() {
        log.info("BatchScheduler -> Running scheduled task: checkExpiredBatches at 6:00 AM");
        try {
            batchService.checkExpiredBatches();
            log.info("BatchScheduler -> checkExpiredBatches completed successfully");
        } catch (Exception e) {
            log.error("BatchScheduler -> Error in checkExpiredBatches: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Bogota")
    public void checkExpiringSoonBatches() {
        log.info("BatchScheduler -> Running scheduled task: checkExpiringSoonBatches at 8:00 AM");
        try {
            batchService.checkExpiringSoonBatches();
            log.info("BatchScheduler -> checkExpiringSoonBatches completed successfully");
        } catch (Exception e) {
            log.error("BatchScheduler -> Error in checkExpiringSoonBatches: {}", e.getMessage(), e);
        }
    }
}
