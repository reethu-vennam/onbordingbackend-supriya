package com.sabbpe.repository;

import com.sabbpe.model.RollingReserveLedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RollingReserveLedgerRepository extends JpaRepository<RollingReserveLedgerEntity, String> {

    List<RollingReserveLedgerEntity> findByMerchantIdOrderByReserveDateDesc(String merchantId);

    Page<RollingReserveLedgerEntity> findByMerchantIdOrderByReserveDateDesc(String merchantId, Pageable pageable);

    List<RollingReserveLedgerEntity> findByStatusAndReleaseDateLessThanEqual(String status, LocalDate releaseDate);

    List<RollingReserveLedgerEntity> findByMerchantIdAndStatus(String merchantId, String status);
}
