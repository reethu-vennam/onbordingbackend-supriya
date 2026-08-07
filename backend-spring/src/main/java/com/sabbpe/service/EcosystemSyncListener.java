package com.sabbpe.service;

import com.sabbpe.event.MerchantApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EcosystemSyncListener {

    private final EcosystemSyncService ecosystemSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMerchantApproved(MerchantApprovedEvent event) {
        log.info("Received MerchantApprovedEvent for merchant {}", event.getMerchantId());
        try {
            ecosystemSyncService.sync(event.getMerchantId());
        } catch (Exception e) {
            log.error("Unexpected error during ecosystem sync for merchant {}: {}",
                    event.getMerchantId(), e.getMessage(), e);
        }
    }
}
