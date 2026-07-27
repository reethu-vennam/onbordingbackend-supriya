package com.sabbpe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementService settlementService;

    @Value("${app.settlement.dry-run:false}")
    private boolean dryRun;

    @Scheduled(cron = "0 2 * * * ?", zone = "Asia/Kolkata")
    public void runSettlementBatch() {
        log.info("Scheduled settlement batch starting (dryRun={})", dryRun);
        var result = settlementService.processSettlementBatch(dryRun);
        log.info("Scheduled settlement batch complete: {} processed, {} success, {} failed",
                result.getMerchantsProcessed(), result.getSuccessful(), result.getFailed());
    }
}
