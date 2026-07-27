package com.sabbpe.service;

import com.sabbpe.dto.*;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.*;
import com.sabbpe.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementHistoryRepository settlementHistoryRepository;
    private final RollingReserveLedgerRepository reserveLedgerRepository;
    private final ObjectMapper objectMapper;

    private static final BigDecimal DEFAULT_MDR_PERCENTAGE = new BigDecimal("1.80");
    private static final int RESERVE_RELEASE_BUSINESS_DAYS = 15;

    public BigDecimal getGrossSettlement(List<TransactionEntity> transactions) {
        return transactions.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getMdrDeduction(List<TransactionEntity> transactions) {
        return transactions.stream()
                .map(t -> {
                    BigDecimal amount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
                    return amount.multiply(DEFAULT_MDR_PERCENTAGE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRollingReserve(BigDecimal grossAmount, MerchantProfileEntity config, BigDecimal mdrDeduction) {
        if (config.getRollingReserveEnabled() == null || !config.getRollingReserveEnabled()) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxReserve = grossAmount.subtract(mdrDeduction);
        if (maxReserve.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentageReserve = BigDecimal.ZERO;
        if (config.getRollingReservePercentage() != null) {
            percentageReserve = grossAmount.multiply(config.getRollingReservePercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        BigDecimal fixedReserve = config.getRollingReserveFixedInr() != null
                ? config.getRollingReserveFixedInr() : BigDecimal.ZERO;

        BigDecimal reserve = percentageReserve.max(fixedReserve);
        return reserve.min(maxReserve);
    }

    public BigDecimal getNetSettlement(BigDecimal grossAmount, BigDecimal mdrDeduction, BigDecimal reserveAmount) {
        BigDecimal net = grossAmount.subtract(mdrDeduction).subtract(reserveAmount);
        return net.max(BigDecimal.ZERO);
    }

    public SettlementCalculation calculateSettlement(List<TransactionEntity> transactions, MerchantProfileEntity config) {
        BigDecimal gross = getGrossSettlement(transactions);
        BigDecimal mdr = getMdrDeduction(transactions);
        BigDecimal reserve = getRollingReserve(gross, config, mdr);
        BigDecimal net = getNetSettlement(gross, mdr, reserve);

        return SettlementCalculation.builder()
                .grossAmount(gross)
                .mdrDeduction(mdr)
                .rollingReserve(reserve)
                .netAmount(net)
                .transactionCount(transactions.size())
                .build();
    }

    public List<MerchantProfileEntity> getEligibleMerchants() {
        return merchantProfileRepository.findEligibleForSettlement();
    }

    public List<TransactionEntity> getPendingTransactions(String merchantId) {
        List<SettlementHistoryEntity> settledHistory = settlementHistoryRepository
                .findByMerchantIdAndStatus(merchantId, "processed");

        Set<String> settledRefs = settledHistory.stream()
                .flatMap(sh -> {
                    try {
                        List<Map<String, Object>> refs = objectMapper.readValue(
                                sh.getTransactionRefs(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                        return refs.stream()
                                .filter(r -> r.containsKey("transaction_id"))
                                .map(r -> (String) r.get("transaction_id"));
                    } catch (Exception e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .collect(Collectors.toSet());

        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .filter(t -> "unsettled".equals(t.getSettlementStatus()))
                .filter(t -> !settledRefs.contains(t.getTransactionId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public SettlementRunResponse.MerchantResult processSettlementForMerchant(String merchantId, boolean dryRun) {
        try {
            MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

            MerchantProfileEntity config = merchant;

            List<TransactionEntity> transactions = getPendingTransactions(merchantId);
            if (transactions.isEmpty()) {
                return SettlementRunResponse.MerchantResult.builder()
                        .merchantId(merchantId).success(true)
                        .message("No pending transactions").build();
            }

            SettlementCalculation calc = calculateSettlement(transactions, config);

            if (dryRun) {
                log.info("DRY RUN: Merchant {} would settle {} txn(s), gross={}, net={}",
                        merchantId, transactions.size(), calc.grossAmount, calc.netAmount);
                return SettlementRunResponse.MerchantResult.builder()
                        .merchantId(merchantId).success(true)
                        .message(String.format("DRY RUN: gross=%.2f, mdr=%.2f, reserve=%.2f, net=%.2f",
                                calc.grossAmount, calc.mdrDeduction, calc.rollingReserve, calc.netAmount))
                        .build();
            }

            String batchRef = "SETL-" + LocalDate.now().toString().replace("-", "")
                    + "-" + String.format("%tT", System.currentTimeMillis()).replace(":", "")
                    + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            List<Map<String, Object>> txnRefObjects = transactions.stream()
                    .map(t -> {
                        Map<String, Object> ref = new LinkedHashMap<>();
                        ref.put("mariaDB_id", t.getId());
                        ref.put("transaction_id", t.getTransactionId());
                        ref.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0);
                        ref.put("completed_at", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
                        return ref;
                    })
                    .collect(Collectors.toList());

            String txnRefsJson;
            try {
                txnRefsJson = objectMapper.writeValueAsString(txnRefObjects);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize transaction refs", e);
            }

            SettlementHistoryEntity history = new SettlementHistoryEntity();
            history.setId(UUID.randomUUID().toString());
            history.setMerchantId(merchantId);
            history.setDistributorId(merchant.getDistributorId() != null ? merchant.getDistributorId() : merchantId);
            history.setSettlementBatchRef(batchRef);
            history.setSettlementDate(LocalDate.now());
            history.setSettlementCycleDays(merchant.getSettlementCycleDays() != null ? merchant.getSettlementCycleDays() : 1);
            history.setGrossAmount(calc.grossAmount);
            history.setMdrDeduction(calc.mdrDeduction);
            history.setRollingReserveHeld(calc.rollingReserve);
            history.setNetSettlementAmount(calc.netAmount);
            history.setTransactionCount(transactions.size());
            history.setTransactionRefs(txnRefsJson);
            history.setStatus("pending");
            settlementHistoryRepository.save(history);

            if (calc.rollingReserve.compareTo(BigDecimal.ZERO) > 0) {
                createReserveLedgerEntry(merchantId, merchant.getDistributorId(),
                        batchRef, calc.grossAmount, calc.rollingReserve, config);
            }

            String distributorId = merchant.getDistributorId() != null ? merchant.getDistributorId() : merchantId;
            try {
                incrementMerchantBalances(merchantId, calc.netAmount);
            } catch (Exception e) {
                log.error("Failed to increment balances for merchant {}", merchantId, e);
                history.setStatus("failed");
                history.setFailureReason(e.getMessage());
                settlementHistoryRepository.save(history);
                return SettlementRunResponse.MerchantResult.builder()
                        .merchantId(merchantId).success(false)
                        .error("Balance increment failed: " + e.getMessage()).build();
            }

            history.setStatus("processed");
            history.setProcessedAt(LocalDateTime.now());
            settlementHistoryRepository.save(history);

            lockSettlementTerms(merchantId);

            log.info("Settlement completed for merchant {}: batch={}, net={}",
                    merchantId, batchRef, calc.netAmount);

            return SettlementRunResponse.MerchantResult.builder()
                    .merchantId(merchantId).success(true)
                    .message(String.format("Settled: gross=%.2f, net=%.2f, batch=%s",
                            calc.grossAmount, calc.netAmount, batchRef))
                    .build();
        } catch (Exception e) {
            log.error("Settlement failed for merchant {}", merchantId, e);
            return SettlementRunResponse.MerchantResult.builder()
                    .merchantId(merchantId).success(false)
                    .error(e.getMessage()).build();
        }
    }

    @Transactional
    public SettlementRunResponse processSettlementBatch(boolean dryRun) {
        List<MerchantProfileEntity> merchants = getEligibleMerchants();
        log.info("Starting settlement batch for {} eligible merchants (dryRun={})", merchants.size(), dryRun);

        List<SettlementRunResponse.MerchantResult> results = new ArrayList<>();
        int success = 0, failed = 0;

        for (MerchantProfileEntity merchant : merchants) {
            SettlementRunResponse.MerchantResult result = processSettlementForMerchant(merchant.getId(), dryRun);
            results.add(result);
            if (result.isSuccess()) success++;
            else failed++;
        }

        log.info("Settlement batch complete: {} processed, {} success, {} failed",
                merchants.size(), success, failed);

        return SettlementRunResponse.builder()
                .dryRun(dryRun)
                .merchantsProcessed(merchants.size())
                .successful(success)
                .failed(failed)
                .results(results)
                .build();
    }

    @Transactional
    public void createReserveLedgerEntry(String merchantId, String distributorId,
                                          String batchRef,
                                          BigDecimal grossAmount, BigDecimal reserveAmount,
                                          MerchantProfileEntity config) {
        LocalDate releaseDate = calculateReleaseDate(LocalDate.now(), RESERVE_RELEASE_BUSINESS_DAYS);
        short cycleDays = config.getSettlementCycleDays() != null ? config.getSettlementCycleDays() : 1;

        RollingReserveLedgerEntity entry = new RollingReserveLedgerEntity();
        entry.setId(UUID.randomUUID().toString());
        entry.setMerchantId(merchantId);
        entry.setDistributorId(distributorId != null ? distributorId : merchantId);
        entry.setTransactionRef(batchRef);
        entry.setGrossSettlementAmount(grossAmount);
        entry.setReserveAmount(reserveAmount);
        entry.setReserveDate(LocalDate.now());
        entry.setReleaseDate(releaseDate);
        entry.setStatus("held");
        entry.setSettlementCycleDays(cycleDays);
        reserveLedgerRepository.save(entry);
    }

    @Transactional
    public void lockSettlementTerms(String merchantId) {
        merchantProfileRepository.findById(merchantId).ifPresent(m -> {
            m.setSettlementTermsLocked(true);
            merchantProfileRepository.save(m);
        });
    }

    @Transactional
    public int releaseReserve() {
        List<RollingReserveLedgerEntity> heldEntries = reserveLedgerRepository
                .findByStatusAndReleaseDateLessThanEqual("held", LocalDate.now());

        log.info("Releasing {} reserve entries due on or before {}", heldEntries.size(), LocalDate.now());

        int released = 0;
        for (RollingReserveLedgerEntity entry : heldEntries) {
            try {
                entry.setStatus("released");
                entry.setReleasedAt(LocalDateTime.now());
                reserveLedgerRepository.save(entry);
                released++;
            } catch (Exception e) {
                log.error("Failed to release reserve entry {}", entry.getId(), e);
            }
        }

        log.info("Released {} reserve entries", released);
        return released;
    }

    public List<SettlementHistoryEntity> getSettlementHistory(String merchantId, int page, int limit) {
        Page<SettlementHistoryEntity> result = settlementHistoryRepository
                .findByMerchantIdOrderBySettlementDateDesc(merchantId, PageRequest.of(page, limit));
        return result.getContent();
    }

    public SettlementSummaryResponse getSettlementSummary(String merchantId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        List<SettlementHistoryEntity> history = settlementHistoryRepository
                .findByMerchantIdOrderBySettlementDateDesc(merchantId);

        return SettlementSummaryResponse.builder()
                .totalGrossSettled(history.stream().map(SettlementHistoryEntity::getGrossAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalMdrDeducted(history.stream().map(SettlementHistoryEntity::getMdrDeduction)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalReserveHeld(history.stream().map(SettlementHistoryEntity::getRollingReserveHeld)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalNetSettled(history.stream().map(SettlementHistoryEntity::getNetSettlementAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalSettlements(history.size())
                .pendingSettlementAmount(merchant.getPendingSettlementAmount())
                .totalSettledAmount(merchant.getTotalSettledAmount())
                .build();
    }

    public List<RollingReserveLedgerEntity> getReserveLedger(String merchantId, int page, int limit) {
        Page<RollingReserveLedgerEntity> result = reserveLedgerRepository
                .findByMerchantIdOrderByReserveDateDesc(merchantId, PageRequest.of(page, limit));
        return result.getContent();
    }

    public SettlementPreviewResponse previewSettlement(String merchantId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        List<TransactionEntity> transactions = getPendingTransactions(merchantId);
        SettlementCalculation calc = calculateSettlement(transactions, merchant);

        List<SettlementPreviewResponse.TransactionItem> items = transactions.stream()
                .map(t -> SettlementPreviewResponse.TransactionItem.builder()
                        .transactionId(t.getTransactionId())
                        .amount(t.getAmount())
                        .status(t.getStatus())
                        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return SettlementPreviewResponse.builder()
                .merchantId(merchantId)
                .merchantName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .transactions(items)
                .grossAmount(calc.grossAmount)
                .mdrDeduction(calc.mdrDeduction)
                .rollingReserve(calc.rollingReserve)
                .netAmount(calc.netAmount)
                .transactionCount(transactions.size())
                .reserveEnabled(merchant.getRollingReserveEnabled() != null && merchant.getRollingReserveEnabled())
                .reservePercentage(merchant.getRollingReservePercentage())
                .settlementCycleDays(merchant.getSettlementCycleDays() != null ? merchant.getSettlementCycleDays() : 1)
                .build();
    }

    @Transactional
    public void incrementMerchantBalances(String merchantId, BigDecimal netAmount) {
        merchantProfileRepository.findById(merchantId).ifPresent(m -> {
            m.setPendingSettlementAmount(m.getPendingSettlementAmount().add(netAmount));
            m.setTotalSettledAmount(m.getTotalSettledAmount().add(netAmount));
            m.setLastSettledAt(LocalDateTime.now());
            merchantProfileRepository.save(m);
        });
    }

    private LocalDate calculateReleaseDate(LocalDate from, int businessDays) {
        LocalDate date = from;
        int added = 0;
        while (added < businessDays) {
            date = date.plusDays(1);
            if (date.getDayOfWeek().getValue() <= 5) {
                added++;
            }
        }
        return date;
    }

    @lombok.Data
    @lombok.Builder
    public static class SettlementCalculation {
        private BigDecimal grossAmount;
        private BigDecimal mdrDeduction;
        private BigDecimal rollingReserve;
        private BigDecimal netAmount;
        private int transactionCount;
    }
}
