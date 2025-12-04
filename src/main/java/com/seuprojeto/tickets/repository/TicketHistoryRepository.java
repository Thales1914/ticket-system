package com.seuprojeto.tickets.repository;

import com.seuprojeto.tickets.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    List<TicketHistory> findByTicketIdOrderByTimestampDesc(Long ticketId);
}
