package com.seuprojeto.tickets.repository;

import com.seuprojeto.tickets.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
}
