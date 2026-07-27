package com.sabbpe.repository;

import com.sabbpe.model.MerchantInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantInvitationRepository extends JpaRepository<MerchantInvitationEntity, String> {

    List<MerchantInvitationEntity> findByDistributorIdOrderByCreatedAtDesc(String distributorId);

    Optional<MerchantInvitationEntity> findByInvitationToken(String token);

    long countByDistributorIdAndStatus(String distributorId, String status);
}
