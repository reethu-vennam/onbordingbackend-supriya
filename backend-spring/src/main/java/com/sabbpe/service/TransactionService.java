package com.sabbpe.service;

import com.sabbpe.dto.*;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.model.TransactionEntity;
import com.sabbpe.repository.MerchantProfileRepository;
import com.sabbpe.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> storeTransactionId(String userId, String transactionId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));

        String existing = merchant.getTransactionId();
        if (existing != null && existing.equals(transactionId)) {
            log.info("Transaction ID already same for merchant {}, skipping", userId);
            return Map.of("outcome", "skipped", "merchantId", merchant.getId(), "transactionId", transactionId);
        }

        merchant.setTransactionId(transactionId);
        merchant.setTxnDetails(null);
        merchantProfileRepository.save(merchant);
        log.info("Stored transaction_id {} for merchant {} (previous was {})", transactionId, userId, existing);
        return Map.of("outcome", "stored", "merchantId", merchant.getId(), "transactionId", transactionId);
    }

    @Transactional
    public Map<String, Object> storeTxnDetails(String transactionId, String txnDetailsJson) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByTransactionId(transactionId)
                .orElse(null);

        if (merchant == null) {
            return Map.of("outcome", "not_found", "transactionId", transactionId);
        }

        if (merchant.getTxnDetails() != null) {
            log.info("Txn details already stored for transaction_id {}, skipping", transactionId);
            return Map.of("outcome", "skipped", "merchantId", merchant.getId(), "transactionId", transactionId);
        }

        merchant.setTxnDetails(txnDetailsJson);
        merchantProfileRepository.save(merchant);
        log.info("Stored txn_details for transaction_id {}", transactionId);
        return Map.of("outcome", "stored", "merchantId", merchant.getId(), "transactionId", transactionId);
    }

    public String getTransactionId(String userId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
        return merchant.getTransactionId();
    }

    public Map<String, Object> getMerchantTransactionId(String userId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
        return Map.of("transactionId", merchant.getTransactionId() != null ? merchant.getTransactionId() : "");
    }

    public Map<String, Object> getTransactionDetails(String userId, String transactionId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));

        if (merchant.getTransactionId() == null || !merchant.getTransactionId().equals(transactionId)) {
            throw new ResourceNotFoundException("Merchant", "transactionId", transactionId);
        }

        return Map.of(
                "merchantId", merchant.getId(),
                "transactionId", merchant.getTransactionId(),
                "txnDetails", merchant.getTxnDetails() != null ? merchant.getTxnDetails() : ""
        );
    }

    public String extractTransactionIdFromPayload(Object payload) {
        try {
            JsonNode node = objectMapper.valueToTree(payload);
            String[] keys = {"transaction_id", "master_transaction_id", "txnid", "txn_id"};
            for (String key : keys) {
                if (node.has(key) && !node.get(key).isNull()) {
                    return node.get(key).asText();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract transaction ID from payload", e);
        }
        return null;
    }

    @Transactional
    public TransactionResponse recordTransaction(TransactionWebhookRequest request) {
        String merchantId = resolveMerchantId(request.getMerchantId());

        if (transactionRepository.existsByTransactionId(request.getTransactionId())) {
            log.warn("Duplicate transaction {} received, skipping", request.getTransactionId());
            return null;
        }

        TransactionEntity txn = new TransactionEntity();
        txn.setId(UUID.randomUUID().toString());
        txn.setMerchantId(merchantId);
        txn.setTransactionId(request.getTransactionId());
        txn.setAmount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);
        txn.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        txn.setStatus(request.getStatus() != null ? request.getStatus() : "completed");
        txn.setPaymentMethod(request.getPaymentMethod());
        txn.setCustomerName(request.getCustomerName());
        txn.setCustomerEmail(request.getCustomerEmail());
        txn.setCustomerMobile(request.getCustomerMobile());
        txn.setMetadata(request.getMetadata());
        txn.setSettlementStatus("unsettled");
        txn = transactionRepository.save(txn);

        return toResponse(txn);
    }

    public TransactionResponse getTransaction(String txnId) {
        TransactionEntity txn = transactionRepository.findByTransactionId(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionId", txnId));
        return toResponse(txn);
    }

    public List<TransactionResponse> getMerchantTransactions(String merchantId) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String resolveMerchantId(String identifier) {
        if (identifier == null) return null;
        Optional<MerchantProfileEntity> byId = merchantProfileRepository.findById(identifier);
        if (byId.isPresent()) return byId.get().getId();
        Optional<MerchantProfileEntity> byTxnId = merchantProfileRepository.findByTransactionId(identifier);
        if (byTxnId.isPresent()) return byTxnId.get().getId();
        return identifier;
    }

    private TransactionResponse toResponse(TransactionEntity t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .merchantId(t.getMerchantId())
                .txnId(t.getTransactionId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .paymentMethod(t.getPaymentMethod())
                .customerName(t.getCustomerName())
                .customerEmail(t.getCustomerEmail())
                // settlementStatus field removed from TransactionResponse builder or named differently;
                // omit mapping here to match TransactionResponse definition
                .createdAt(t.getCreatedAt())
                .build();
    }
}
