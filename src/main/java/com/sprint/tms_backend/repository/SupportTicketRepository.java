package com.sprint.tms_backend.repository;

import com.sprint.tms_backend.model.SupportTicket;
import com.sprint.tms_backend.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
    List<SupportTicket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
    List<SupportTicket> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId);
    long countByStatus(TicketStatus status);
}