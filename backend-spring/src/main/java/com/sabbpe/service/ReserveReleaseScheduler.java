package com.sabbpe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveReleaseScheduler {

    private final SettlementService settlementService;

    @Scheduled(cron = "0 3 * * * ?", zone = "Asia/Kolkata")
    public void runReserveRelease() {
        log.info("Scheduled reserve release starting");
        int released = settlementService.releaseReserve();
        log.info("Scheduled reserve release complete: {} entries released", released);
    }
}
