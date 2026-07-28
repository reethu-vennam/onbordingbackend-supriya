package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.MerchantProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfileEntity, String> {
    List<MerchantProfileEntity> findAllByUserId(String userId);

    // findByUserId assumes a single profile per user, but that isn't enforced at the DB level
    // (ddl-auto: none), so more than one row can share a user_id. Spring Data's derived
    // single-result query throws IncorrectResultSizeDataAccessException in that case, which
    // callers weren't expecting - route through the list query and take the first match instead.
    default Optional<MerchantProfileEntity> findByUserId(String userId) {
        List<MerchantProfileEntity> matches = findAllByUserId(userId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    Optional<MerchantProfileEntity> findByBankApplicationId(String bankApplicationId);
    java.util.List<MerchantProfileEntity> findByOnboardingStatus(String status);
}
