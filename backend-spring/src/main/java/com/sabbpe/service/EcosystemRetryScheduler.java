package com.sabbpe.service;

import com.sabbpe.model.EcosystemSyncLogEntity;
import com.sabbpe.repository.EcosystemSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EcosystemRetryScheduler {

    private final EcosystemSyncLogRepository syncLogRepository;
    private final EcosystemSyncService ecosystemSyncService;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Scheduled(cron = "0 */5 * * * ?", zone = "Asia/Kolkata")
    public void retryFailedSyncs() {
        log.debug("Ecosystem retry scheduler running");
        List<EcosystemSyncLogEntity> failedLogs = syncLogRepository
                .findByStatusAndAttemptCountLessThan("FAILED", MAX_RETRY_ATTEMPTS);

        if (failedLogs.isEmpty()) {
            log.debug("No failed syncs to retry");
            return;
        }

        log.info("Retrying {} failed ecosystem sync(s)", failedLogs.size());
        for (EcosystemSyncLogEntity logEntry : failedLogs) {
            try {
                ecosystemSyncService.retry(logEntry.getMerchantId());
            } catch (Exception e) {
                log.error("Retry failed for merchant {}: {}",
                        logEntry.getMerchantId(), e.getMessage());
            }
        }
    }
}
