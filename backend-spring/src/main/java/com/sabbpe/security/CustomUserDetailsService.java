package com.sabbpe.security;

import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.model.UserEntity;
import com.sabbpe.model.UserRoleEntity;
import com.sabbpe.repository.MerchantProfileRepository;
import com.sabbpe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<String> roles = user.getRoles().stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());

        String merchantId = null;
        try {
            MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(user.getId()).orElse(null);
            if (merchant != null) {
                merchantId = merchant.getId();
            }
        } catch (Exception e) {
            log.debug("No merchant profile for user {}", user.getId());
        }

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                roles,
                user.getIsActive(),
                merchantId
        );
    }

    public UserDetails loadUserById(String userId) {
        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        List<String> roles = user.getRoles().stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toList());

        String merchantId = null;
        try {
            MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(user.getId()).orElse(null);
            if (merchant != null) {
                merchantId = merchant.getId();
            }
        } catch (Exception e) {
            log.debug("No merchant profile for user {}", user.getId());
        }

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                roles,
                user.getIsActive(),
                merchantId
        );
    }
}
