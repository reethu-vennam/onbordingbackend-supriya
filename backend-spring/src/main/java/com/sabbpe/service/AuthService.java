package com.sabbpe.service;

import com.sabbpe.dto.AuthResponse;
import com.sabbpe.dto.LoginRequest;
import com.sabbpe.dto.RegisterRequest;
import com.sabbpe.dto.UserDto;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.UnauthorizedException;
import com.sabbpe.model.*;
import com.sabbpe.repository.*;
import com.sabbpe.security.CustomUserDetails;
import com.sabbpe.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final DistributorProfileRepository distributorProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setMobileNumber(request.getMobileNumber());
        user.setIsActive(true);
        user = userRepository.save(user);

        UserRoleEntity role = new UserRoleEntity();
        role.setId(UUID.randomUUID().toString());
        role.setUser(user);
        role.setRoleId(request.getRole().toLowerCase());
        userRoleRepository.save(role);

        user.setRoles(Set.of(role));

        List<String> roles = List.of(request.getRole().toLowerCase());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        String merchantId = null;

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .merchantId(merchantId)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            List<String> roles = userDetails.getRoles();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    userDetails.getId(), userDetails.getEmail(), roles);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getId());

            return AuthResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .userId(userDetails.getId())
                    .email(userDetails.getEmail())
                    .fullName(userDetails.getUsername())
                    .roles(roles)
                    .merchantId(userDetails.getMerchantId())
                    .build();
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    public UserDto getCurrentUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<String> roles = userRoleRepository.findByUserId(userId)
                .stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .isActive(user.getIsActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void updatePassword(String userId, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
