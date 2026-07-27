package com.sabbpe.admin.service;

import com.sabbpe.admin.exception.BadRequestException;
import com.sabbpe.admin.exception.UnauthorizedException;
import com.sabbpe.admin.model.*;
import com.sabbpe.admin.repository.*;
import com.sabbpe.admin.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Map<String, Object> register(String email, String password, String fullName, String role) {
        if (userRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new BadRequestException("Email already registered");
        }
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        UserRoleEntity userRole = UserRoleEntity.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .roleId(role != null ? role.toLowerCase() : "support")
                .build();
        userRoleRepository.save(userRole);

        List<String> roles = List.of(userRole.getRoleId());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roles);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", buildUserMap(user, roles));
        return result;
    }

    public Map<String, Object> login(String email, String password) {
        UserEntity user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Account disabled");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        List<String> roles = userRoleRepository.findByUserId(user.getId())
                .stream().map(UserRoleEntity::getRoleId).collect(Collectors.toList());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roles);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", buildUserMap(user, roles));
        return result;
    }

    public List<Map<String, Object>> getSupportUsers() {
        return userRoleRepository.findByRoleId("support").stream()
                .map(UserRoleEntity::getUser)
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("role", "support");
                    map.put("is_active", user.getIsActive());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void createSupportUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new BadRequestException("Email already registered");
        }
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password))
                .fullName(name)
                .isActive(true)
                .build();
        user = userRepository.save(user);
        userRoleRepository.save(UserRoleEntity.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .roleId("support")
                .build());
    }

    @Transactional
    public void toggleSupportStatus(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteSupportUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        userRepository.delete(user);
    }

    private Map<String, Object> buildUserMap(UserEntity user, List<String> roles) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("name", user.getFullName());
        map.put("role", roles.isEmpty() ? "support" : roles.get(0));
        map.put("is_active", user.getIsActive());
        return map;
    }
}
