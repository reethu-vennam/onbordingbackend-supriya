package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, String> {
    Page<TicketEntity> findByAssignedToOrderByCreatedAtDesc(String assignedTo, Pageable pageable);
    Page<TicketEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<TicketEntity> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);
    Page<TicketEntity> findByAssignedToAndStatusOrderByCreatedAtDesc(String assignedTo, String status, Pageable pageable);
    List<TicketEntity> findByCreatedByAndModuleOrderByCreatedAtDesc(String createdBy, String module);
    List<TicketEntity> findByModuleAndAssignedTo(String module, String assignedTo);
    @Query("SELECT t.status, COUNT(t) FROM TicketEntity t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();
    long countByStatus(String status);
}
