package com.sabbpe.service;

import com.sabbpe.dto.CreateEmployeeRequest;
import com.sabbpe.dto.EmployeeResponse;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.model.EmployeeProfileEntity;
import com.sabbpe.model.UserEntity;
import com.sabbpe.model.UserRoleEntity;
import com.sabbpe.repository.EmployeeProfileRepository;
import com.sabbpe.repository.UserRepository;
import com.sabbpe.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (employeeProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Employee email already exists");
        }

        String plainPassword = request.getPassword();
        if (plainPassword == null || plainPassword.length() < 6) {
            plainPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setFullName(request.getFullName());
        user.setMobileNumber(request.getMobileNumber());
        user.setIsActive(true);
        user = userRepository.save(user);

        UserRoleEntity role = new UserRoleEntity();
        role.setId(UUID.randomUUID().toString());
        role.setUser(user);
        role.setRoleId("employee");
        userRoleRepository.save(role);

        EmployeeProfileEntity employee = new EmployeeProfileEntity();
        employee.setId(UUID.randomUUID().toString());
        employee.setUserId(user.getId());
        employee.setFullName(request.getFullName());
        employee.setMobileNumber(request.getMobileNumber());
        employee.setEmail(request.getEmail());
        employee.setIsActive(true);
        employee = employeeProfileRepository.save(employee);

        try {
            notificationService.sendEmployeeCredentialsEmail(
                    request.getEmail(), request.getFullName(), plainPassword);
        } catch (Exception e) {
            log.warn("Failed to send employee credentials email to {}: {}", request.getEmail(), e.getMessage());
        }

        return buildResponse(employee, plainPassword);
    }

    public List<EmployeeResponse> listEmployees() {
        return employeeProfileRepository.findByIsActiveTrue()
                .stream()
                .map(e -> buildResponse(e, null))
                .collect(Collectors.toList());
    }

    private EmployeeResponse buildResponse(EmployeeProfileEntity e, String tempPassword) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .fullName(e.getFullName())
                .mobileNumber(e.getMobileNumber())
                .email(e.getEmail())
                .isActive(e.getIsActive() != null && e.getIsActive())
                .createdAt(e.getCreatedAt())
                .tempPassword(tempPassword)
                .build();
    }
}
