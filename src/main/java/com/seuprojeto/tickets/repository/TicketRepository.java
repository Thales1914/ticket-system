package com.seuprojeto.tickets.repository;

import com.seuprojeto.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
