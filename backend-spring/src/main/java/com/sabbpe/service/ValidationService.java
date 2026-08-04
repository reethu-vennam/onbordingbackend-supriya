package com.sabbpe.service;

import com.sabbpe.dto.MerchantProfileRequest;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.model.MerchantProfileEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[\\d\\s\\-()]+$");
    private static final Pattern IFSC_PATTERN =
            Pattern.compile("^[A-Za-z]{4}0[A-Za-z0-9]{6}$");
    private static final Pattern PAN_PATTERN =
            Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern GST_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PINCODE_PATTERN =
            Pattern.compile("^[1-9][0-9]{5}$");

    public void validateForSubmission(MerchantProfileEntity merchant) {
        List<String> errors = new ArrayList<>();

        validateBusinessInfo(merchant, errors);
        validateContactInfo(merchant, errors);
        validateAddress(merchant, errors);

        if (!errors.isEmpty()) {
            throw new BadRequestException("Validation failed", errors);
        }
    }

    public void validateBusinessInfo(MerchantProfileEntity m, List<String> errors) {
        if (m.getBusinessName() == null || m.getBusinessName().length() < 2) {
            errors.add("Business name must be at least 2 characters");
        }
        if (m.getEntityType() == null || m.getEntityType().isBlank()) {
            errors.add("Entity type is required");
        }
        if (m.getBusinessAddressLine1() == null || m.getBusinessAddressLine1().isBlank()) {
            errors.add("Business address line 1 is required");
        }
        if (m.getBusinessCity() == null || m.getBusinessCity().isBlank()) {
            errors.add("Business city is required");
        }
        if (m.getBusinessState() == null || m.getBusinessState().isBlank()) {
            errors.add("Business state is required");
        }
        if (m.getBusinessPostalCode() == null || m.getBusinessPostalCode().isBlank()) {
            errors.add("Business postal code is required");
        } else if (!PINCODE_PATTERN.matcher(m.getBusinessPostalCode()).matches()) {
            errors.add("Invalid postal code format");
        }
        if (m.getBusinessCountry() == null || m.getBusinessCountry().isBlank()) {
            errors.add("Business country is required");
        }
        if (m.getGstNumber() != null && !m.getGstNumber().isBlank()
                && !GST_PATTERN.matcher(m.getGstNumber()).matches()) {
            errors.add("Invalid GST number format");
        }
    }

    public void validateContactInfo(MerchantProfileEntity m, List<String> errors) {
        if (m.getEmail() == null || !EMAIL_PATTERN.matcher(m.getEmail()).matches()) {
            errors.add("Invalid email format");
        }
        if (m.getMobileNumber() == null || !PHONE_PATTERN.matcher(m.getMobileNumber()).matches()) {
            errors.add("Invalid phone number format");
        }
        if (m.getPanNumber() != null && !m.getPanNumber().isBlank()
                && !PAN_PATTERN.matcher(m.getPanNumber()).matches()) {
            errors.add("Invalid PAN number format");
        }
    }

    public void validateAddress(MerchantProfileEntity m, List<String> errors) {
    }

    public void validateBankDetailsRequest(String accountNumber, String ifscCode,
                                            String accountHolderName, String bankName) {
        List<String> errors = new ArrayList<>();
        if (accountNumber == null || accountNumber.length() < 9) {
            errors.add("Account number must be at least 9 digits");
        }
        if (ifscCode == null || !IFSC_PATTERN.matcher(ifscCode).matches()) {
            errors.add("Invalid IFSC code format (e.g., SBIN0123456)");
        }
        if (accountHolderName == null || accountHolderName.isBlank()) {
            errors.add("Account holder name is required");
        }
        if (bankName == null || bankName.isBlank()) {
            errors.add("Bank name is required");
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException("Bank validation failed", errors);
        }
    }
}
