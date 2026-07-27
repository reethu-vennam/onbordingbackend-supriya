package com.sabbpe.repository;

import com.sabbpe.model.DistributorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributorProfileRepository extends JpaRepository<DistributorProfileEntity, String> {

    Optional<DistributorProfileEntity> findByUserId(String userId);

    Optional<DistributorProfileEntity> findByEmail(String email);

    Optional<DistributorProfileEntity> findByOnboardingToken(String onboardingToken);

    boolean existsByEmail(String email);
}
