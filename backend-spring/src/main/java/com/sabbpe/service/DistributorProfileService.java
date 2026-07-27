package com.sabbpe.service;

import com.sabbpe.dto.DistributorProfileRequest;
import com.sabbpe.dto.DistributorProfileResponse;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.DistributorProfileEntity;
import com.sabbpe.model.UserEntity;
import com.sabbpe.repository.DistributorProfileRepository;
import com.sabbpe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributorProfileService {

    private final DistributorProfileRepository distributorProfileRepository;
    private final UserRepository userRepository;

    public DistributorProfileEntity getByUserId(String userId) {
        return distributorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor profile", "userId", userId));
    }

    public DistributorProfileResponse getProfile(String userId) {
        DistributorProfileEntity profile = getByUserId(userId);
        return buildResponse(profile);
    }

    @Transactional
    public DistributorProfileResponse createProfile(String userId, DistributorProfileRequest request) {
        if (distributorProfileRepository.findByUserId(userId).isPresent()) {
            return updateProfile(userId, request);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DistributorProfileEntity profile = new DistributorProfileEntity();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId);
        applyRequest(profile, request);
        profile = distributorProfileRepository.save(profile);

        return buildResponse(profile);
    }

    @Transactional
    public DistributorProfileResponse updateProfile(String userId, DistributorProfileRequest request) {
        DistributorProfileEntity profile = getByUserId(userId);

        // Validate payout_cycle
        if (request.getPayoutCycle() != null) {
            if (!DistributorProfileRequest.VALID_PAYOUT_CYCLES.contains(request.getPayoutCycle())) {
                throw new BadRequestException("Invalid payout_cycle. Must be one of: " +
                        String.join(", ", DistributorProfileRequest.VALID_PAYOUT_CYCLES));
            }
        }

        // Validate default_commission_rate
        if (request.getDefaultCommissionRate() != null) {
            if (request.getDefaultCommissionRate().compareTo(BigDecimal.ZERO) < 0 ||
                    request.getDefaultCommissionRate().compareTo(new BigDecimal("100")) > 0) {
                throw new BadRequestException("default_commission_rate must be between 0 and 100");
            }
        }

        // Track bank field changes
        boolean bankFieldsChanged = false;
        if (request.getBankAccountHolder() != null) { profile.setBankAccountHolder(request.getBankAccountHolder()); bankFieldsChanged = true; }
        if (request.getBankName() != null) { profile.setBankName(request.getBankName()); bankFieldsChanged = true; }
        if (request.getBankAccountNumber() != null) { profile.setBankAccountNumber(request.getBankAccountNumber()); bankFieldsChanged = true; }
        if (request.getBankIfsc() != null) { profile.setBankIfsc(request.getBankIfsc()); bankFieldsChanged = true; }

        applyRequest(profile, request);

        if (bankFieldsChanged) {
            profile.setBankUpdatedAt(LocalDateTime.now());
        }

        profile = distributorProfileRepository.save(profile);
        return buildResponse(profile);
    }

    private void applyRequest(DistributorProfileEntity profile, DistributorProfileRequest r) {
        if (r.getCompanyName() != null) profile.setCompanyName(r.getCompanyName());
        if (r.getContactPerson() != null) profile.setContactPerson(r.getContactPerson());
        if (r.getEmail() != null) profile.setEmail(r.getEmail());
        if (r.getMobileNumber() != null) profile.setMobileNumber(r.getMobileNumber());
        if (r.getTerritory() != null) profile.setTerritory(r.getTerritory());
        if (r.getAddress() != null) profile.setAddress(r.getAddress());
        if (r.getCity() != null) profile.setCity(r.getCity());
        if (r.getState() != null) profile.setState(r.getState());
        if (r.getPincode() != null) profile.setPincode(r.getPincode());
        if (r.getPanNumber() != null) profile.setPanNumber(r.getPanNumber());
        if (r.getAadhaarLast4() != null) profile.setAadhaarLast4(r.getAadhaarLast4());
        if (r.getPanDocumentPath() != null) profile.setPanDocumentPath(r.getPanDocumentPath());
        if (r.getAadhaarDocumentPath() != null) profile.setAadhaarDocumentPath(r.getAadhaarDocumentPath());
        if (r.getProfilePhotoPath() != null) profile.setProfilePhotoPath(r.getProfilePhotoPath());
        if (r.getDefaultCommissionRate() != null) profile.setDefaultCommissionRate(r.getDefaultCommissionRate());
        if (r.getPayoutCycle() != null) profile.setPayoutCycle(r.getPayoutCycle());
    }

    private DistributorProfileResponse buildResponse(DistributorProfileEntity p) {
        return DistributorProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .companyName(p.getCompanyName())
                .contactPerson(p.getContactPerson())
                .email(p.getEmail())
                .mobileNumber(p.getMobileNumber())
                .territory(p.getTerritory())
                .isActive(p.getIsActive() != null && p.getIsActive())
                .address(p.getAddress())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .bankAccountHolder(p.getBankAccountHolder())
                .bankName(p.getBankName())
                .bankAccountNumber(p.getBankAccountNumber())
                .bankIfsc(p.getBankIfsc())
                .panNumber(p.getPanNumber())
                .aadhaarLast4(p.getAadhaarLast4())
                .panVerified(p.getPanVerified() != null && p.getPanVerified())
                .aadhaarVerified(p.getAadhaarVerified() != null && p.getAadhaarVerified())
                .bankVerified(p.getBankVerified() != null && p.getBankVerified())
                .panDocumentPath(p.getPanDocumentPath())
                .aadhaarDocumentPath(p.getAadhaarDocumentPath())
                .profilePhotoPath(p.getProfilePhotoPath())
                .signedAgreementPath(p.getSignedAgreementPath())
                .defaultCommissionRate(p.getDefaultCommissionRate())
                .payoutCycle(p.getPayoutCycle())
                .securityDeposit(p.getSecurityDeposit())
                .availableRecoveryBalance(p.getAvailableRecoveryBalance())
                .agreementStatus(p.getAgreementStatus())
                .kycStatus(p.getKycStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
