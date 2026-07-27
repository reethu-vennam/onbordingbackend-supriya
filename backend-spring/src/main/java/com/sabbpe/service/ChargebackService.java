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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargebackService {

    private final ChargebackRepository chargebackRepository;
    private final ChargebackHistoryRepository chargebackHistoryRepository;
    private final DistributorRecoveryHistoryRepository distributorRecoveryHistoryRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final DistributorProfileRepository distributorProfileRepository;
    private final RollingReserveLedgerRepository reserveLedgerRepository;
    private final SettlementHistoryRepository settlementHistoryRepository;
    private final ObjectMapper objectMapper;

    public ChargebackEntity getChargeback(String chargebackId) {
        return chargebackRepository.findById(chargebackId)
                .orElseThrow(() -> new ResourceNotFoundException("Chargeback", "id", chargebackId));
    }

    public ChargebackResponse getChargebackResponse(String chargebackId) {
        return toResponse(getChargeback(chargebackId));
    }

    @Transactional
    public ChargebackResponse createChargeback(CreateChargebackRequest request, String performedBy) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getMerchantId()));

        if (merchant.getPendingChargebackAmount() == null) {
            merchant.setPendingChargebackAmount(BigDecimal.ZERO);
        }

        ChargebackEntity chargeback = new ChargebackEntity();
        chargeback.setId(UUID.randomUUID().toString());
        chargeback.setMerchantId(request.getMerchantId());
        chargeback.setAmount(request.getAmount());
        chargeback.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        chargeback.setReason(request.getReason());
        chargeback.setStatus("pending");
        chargeback.setMetadata(request.getMetadata());
        chargeback = chargebackRepository.save(chargeback);

        merchant.setTotalChargebackAmount(merchant.getTotalChargebackAmount().add(request.getAmount()));
        merchant.setPendingChargebackAmount(merchant.getPendingChargebackAmount().add(request.getAmount()));
        merchantProfileRepository.save(merchant);

        addHistoryEntry(chargeback.getId(), merchant.getId(), "create", "CHARGEBACK_CREATED",
                null, request.getAmount(), null, performedBy, "Chargeback created");

        log.info("Chargeback {} created for merchant {} amount {}",
                chargeback.getId(), request.getMerchantId(), request.getAmount());

        return toResponse(chargeback);
    }

    @Transactional
    public ChargebackResponse recoverChargeback(String chargebackId, String performedBy) {
        ChargebackEntity chargeback = getChargeback(chargebackId);

        if (!"pending".equals(chargeback.getStatus()) && !"recovering".equals(chargeback.getStatus())) {
            throw new BadRequestException("Chargeback is not in recoverable state: " + chargeback.getStatus());
        }

        chargeback.setStatus("recovering");
        chargebackRepository.save(chargeback);

        MerchantProfileEntity merchant = merchantProfileRepository.findById(chargeback.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", chargeback.getMerchantId()));

        BigDecimal remainingAmount = chargeback.getAmount();
        List<Map<String, Object>> recoverySteps = new ArrayList<>();

        String[] sources = {"rolling_reserve", "pending_settlement", "merchant_balance", "distributor_balance"};

        for (String source : sources) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal recoveredFromSource = BigDecimal.ZERO;

            switch (source) {
                case "rolling_reserve":
                    recoveredFromSource = recoverFromRollingReserve(chargeback, remainingAmount);
                    break;
                case "pending_settlement":
                    recoveredFromSource = recoverFromPendingSettlement(merchant, remainingAmount);
                    break;
                case "merchant_balance":
                    recoveredFromSource = recoverFromMerchantBalance(merchant, remainingAmount);
                    break;
                case "distributor_balance":
                    recoveredFromSource = recoverFromDistributorBalance(chargeback, merchant, remainingAmount);
                    break;
            }

            if (recoveredFromSource.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> step = new HashMap<>();
                step.put("source", source);
                step.put("amount", recoveredFromSource);
                recoverySteps.add(step);

                addHistoryEntry(chargeback.getId(), merchant.getId(), "recover", "RECOVERED_FROM_" + source.toUpperCase(),
                        null, recoveredFromSource, source, performedBy,
                        "Recovered " + recoveredFromSource + " from " + source);

                remainingAmount = remainingAmount.subtract(recoveredFromSource);
            }
        }

        try {
            chargeback.setRecoverySteps(objectMapper.writeValueAsString(recoverySteps));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize recovery steps", e);
        }

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            chargeback.setStatus("recovered");
            chargeback.setRecoveredAt(LocalDateTime.now());
            chargeback.setRecoverySource(
                    recoverySteps.isEmpty() ? "none" : (String) recoverySteps.get(0).get("source"));
            merchant.setChargebackRecoveryAvailable(
                    merchant.getChargebackRecoveryAvailable().add(chargeback.getAmount()));
            merchant.setPendingChargebackAmount(
                    merchant.getPendingChargebackAmount().subtract(chargeback.getAmount()));
        }

        chargebackRepository.save(chargeback);
        merchantProfileRepository.save(merchant);

        return toResponse(chargeback);
    }

    private BigDecimal recoverFromRollingReserve(ChargebackEntity chargeback, BigDecimal remainingAmount) {
        List<RollingReserveLedgerEntity> heldReserves = reserveLedgerRepository
                .findByMerchantIdAndStatus(chargeback.getMerchantId(), "held");

        BigDecimal recovered = BigDecimal.ZERO;
        for (RollingReserveLedgerEntity entry : heldReserves) {
            if (remainingAmount.subtract(recovered).compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal debitAmount = entry.getReserveAmount().min(remainingAmount.subtract(recovered));
            entry.setStatus("debited");
            entry.setDebitReason("chargeback_" + chargeback.getId());
            entry.setDebitedAt(LocalDateTime.now());
            reserveLedgerRepository.save(entry);

            recovered = recovered.add(debitAmount);
        }

        return recovered;
    }

    private BigDecimal recoverFromPendingSettlement(MerchantProfileEntity merchant, BigDecimal remainingAmount) {
        BigDecimal pending = merchant.getPendingSettlementAmount() != null
                ? merchant.getPendingSettlementAmount() : BigDecimal.ZERO;
        BigDecimal debitAmount = pending.min(remainingAmount);

        if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            merchant.setPendingSettlementAmount(pending.subtract(debitAmount));
        }

        return debitAmount;
    }

    private BigDecimal recoverFromMerchantBalance(MerchantProfileEntity merchant, BigDecimal remainingAmount) {
        BigDecimal totalSettled = merchant.getTotalSettledAmount() != null
                ? merchant.getTotalSettledAmount() : BigDecimal.ZERO;
        BigDecimal totalPending = merchant.getPendingSettlementAmount() != null
                ? merchant.getPendingSettlementAmount() : BigDecimal.ZERO;
        BigDecimal available = totalSettled.subtract(totalPending).max(BigDecimal.ZERO);
        BigDecimal debitAmount = available.min(remainingAmount);

        if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            merchant.setTotalSettledAmount(totalSettled.subtract(debitAmount));
        }

        return debitAmount;
    }

    private BigDecimal recoverFromDistributorBalance(ChargebackEntity chargeback,
                                                      MerchantProfileEntity merchant,
                                                      BigDecimal remainingAmount) {
        if (merchant.getDistributorId() == null) return BigDecimal.ZERO;

        DistributorProfileEntity distributor = distributorProfileRepository
                .findByUserId(merchant.getDistributorId()).orElse(null);
        if (distributor == null) return BigDecimal.ZERO;

        BigDecimal available = distributor.getAvailableRecoveryBalance() != null
                ? distributor.getAvailableRecoveryBalance() : BigDecimal.ZERO;
        BigDecimal debitAmount = available.min(remainingAmount);

        if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            distributor.setAvailableRecoveryBalance(available.subtract(debitAmount));
            distributor.setTotalRecoveredAmount(
                    distributor.getTotalRecoveredAmount().add(debitAmount));
            distributorProfileRepository.save(distributor);

            DistributorRecoveryHistoryEntity recovery = new DistributorRecoveryHistoryEntity();
            recovery.setId(UUID.randomUUID().toString());
            recovery.setDistributorId(distributor.getId());
            recovery.setChargebackId(chargeback.getId());
            recovery.setMerchantId(merchant.getId());
            recovery.setAmount(debitAmount);
            distributorRecoveryHistoryRepository.save(recovery);
        }

        return debitAmount;
    }

    private void addHistoryEntry(String chargebackId, String merchantId, String action,
                                  String eventType, String previousData, BigDecimal amount,
                                  String recoverySource, String performedBy, String comments) {
        ChargebackHistoryEntity history = new ChargebackHistoryEntity();
        history.setId(UUID.randomUUID().toString());
        history.setChargebackId(chargebackId);
        history.setMerchantId(merchantId);
        history.setAction(action);
        history.setEventType(eventType);
        history.setPreviousData(previousData);
        history.setRecoveredAmount(amount);
        history.setRecoverySource(recoverySource);
        history.setPerformedBy(performedBy);
        history.setComments(comments);
        chargebackHistoryRepository.save(history);
    }

    public List<ChargebackResponse> getChargebacks(String merchantId, int page, int limit, String status) {
        Page<ChargebackEntity> result;
        if (merchantId != null && !merchantId.isBlank()) {
            result = chargebackRepository.findByMerchantId(merchantId, PageRequest.of(page, limit));
        } else {
            result = chargebackRepository.findAll(PageRequest.of(page, limit));
        }

        List<String> merchantIds = result.getContent().stream()
                .map(ChargebackEntity::getMerchantId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, MerchantProfileEntity> merchantMap = merchantIds.isEmpty()
                ? Map.of()
                : merchantProfileRepository.findAllById(merchantIds).stream()
                        .collect(Collectors.toMap(MerchantProfileEntity::getId, m -> m));

        return result.getContent().stream().map(c -> {
            ChargebackResponse resp = toResponse(c);
            MerchantProfileEntity m = merchantMap.get(c.getMerchantId());
            resp.setMerchantName(m != null
                    ? (m.getFullName() != null ? m.getFullName()
                            : m.getBusinessName() != null ? m.getBusinessName()
                            : m.getEmail() != null ? m.getEmail() : "\u2014")
                    : "\u2014");
            return resp;
        }).collect(Collectors.toList());
    }

    public List<ChargebackHistoryResponse> getChargebackHistory(String chargebackId, int page, int limit) {
        Page<ChargebackHistoryEntity> result = chargebackHistoryRepository
                .findByChargebackIdOrderByEventTimestampDesc(chargebackId, PageRequest.of(page, limit));
        return result.getContent().stream()
                .map(h -> ChargebackHistoryResponse.builder()
                        .id(h.getId())
                        .chargebackId(h.getChargebackId())
                        .action(h.getAction())
                        .eventType(h.getEventType())
                        .recoveredAmount(h.getRecoveredAmount())
                        .recoverySource(h.getRecoverySource())
                        .eventTimestamp(h.getEventTimestamp())
                        .performedBy(h.getPerformedBy())
                        .comments(h.getComments())
                        .build())
                .collect(Collectors.toList());
    }

    public ChargebackSummaryResponse getChargebackSummary(String merchantId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        List<ChargebackEntity> chargebacks = chargebackRepository
                .findByMerchantIdOrderByChargebackDateDesc(merchantId);

        long pending = chargebacks.stream().filter(c -> "pending".equals(c.getStatus())).count();
        long recovered = chargebacks.stream().filter(c -> "recovered".equals(c.getStatus())).count();

        return ChargebackSummaryResponse.builder()
                .totalChargebacks(chargebacks.size())
                .pendingChargebacks(pending)
                .recoveredChargebacks(recovered)
                .totalAmount(chargebacks.stream().map(ChargebackEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .pendingAmount(chargebacks.stream()
                        .filter(c -> "pending".equals(c.getStatus()) || "recovering".equals(c.getStatus()))
                        .map(ChargebackEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .recoveredAmount(chargebacks.stream().filter(c -> "recovered".equals(c.getStatus()))
                        .map(ChargebackEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalChargebackAmount(merchant.getTotalChargebackAmount())
                .pendingChargebackAmount(merchant.getPendingChargebackAmount())
                .chargebackRecoveryAvailable(merchant.getChargebackRecoveryAvailable())
                .build();
    }

    public DistributorRecoverySummaryResponse getDistributorRecoverySummary(String distributorId) {
        DistributorProfileEntity distributor = distributorProfileRepository
                .findByUserId(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "userId", distributorId));

        List<DistributorRecoveryHistoryEntity> recoveries = distributorRecoveryHistoryRepository
                .findByDistributorIdOrderByCreatedAtDesc(distributor.getId());

        return DistributorRecoverySummaryResponse.builder()
                .distributorId(distributor.getId())
                .availableRecoveryBalance(distributor.getAvailableRecoveryBalance())
                .totalRecoveredAmount(distributor.getTotalRecoveredAmount())
                .securityDeposit(distributor.getSecurityDeposit())
                .totalRecoveries(recoveries.size())
                .build();
    }

    private ChargebackResponse toResponse(ChargebackEntity c) {
        return ChargebackResponse.builder()
                .id(c.getId())
                .merchantId(c.getMerchantId())
                .amount(c.getAmount())
                .currency(c.getCurrency())
                .reason(c.getReason())
                .status(c.getStatus())
                .chargebackDate(c.getChargebackDate())
                .recoveredAt(c.getRecoveredAt())
                .recoverySource(c.getRecoverySource())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
