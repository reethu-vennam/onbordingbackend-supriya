package com.sabbpe.config;

import com.sabbpe.model.UserEntity;
import com.sabbpe.model.UserRoleEntity;
import com.sabbpe.repository.UserRepository;
import com.sabbpe.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@sabbpe.com";
        String adminPassword = "admin@123";
        String encodedPassword = passwordEncoder.encode(adminPassword);

        log.info("TestDataInitializer running...");

        var existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin.isPresent()) {
            UserEntity admin = existingAdmin.get();
            admin.setPasswordHash(encodedPassword);
            admin.setIsActive(true);
            admin.setFullName("Super Admin");
            userRepository.save(admin);
            log.info("Admin password updated for: {} (id={})", adminEmail, admin.getId());

            boolean hasAdminRole = userRoleRepository.existsByUserIdAndRoleId(admin.getId(), "admin");
            if (!hasAdminRole) {
                UserRoleEntity role = new UserRoleEntity();
                role.setId(UUID.randomUUID().toString());
                role.setUser(admin);
                role.setRoleId("admin");
                userRoleRepository.save(role);
                log.info("Admin role added for user: {}", admin.getId());
            }
        } else {
            log.info("Creating admin user...");

            UserEntity admin = new UserEntity();
            admin.setId(UUID.randomUUID().toString());
            admin.setEmail(adminEmail);
            admin.setPasswordHash(encodedPassword);
            admin.setFullName("Super Admin");
            admin.setMobileNumber("9999999999");
            admin.setIsActive(true);
            admin = userRepository.save(admin);

            UserRoleEntity adminRole = new UserRoleEntity();
            adminRole.setId(UUID.randomUUID().toString());
            adminRole.setUser(admin);
            adminRole.setRoleId("admin");
            userRoleRepository.save(adminRole);

            log.info("Admin user created: {} / {}", adminEmail, adminPassword);
        }

        log.info("TestDataInitializer complete");
    }
}
