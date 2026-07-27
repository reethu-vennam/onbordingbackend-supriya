package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.TicketMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessageEntity, String> {
    List<TicketMessageEntity> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
