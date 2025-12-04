package com.seuprojeto.tickets.repository;

import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCreatedById(Long userId);

    @Query("""
        SELECT t FROM Ticket t
        WHERE (:status IS NULL OR t.status = :status)
        AND   (:priority IS NULL OR t.priority = :priority)
        AND   (:assignedTo IS NULL OR t.assignedTo.id = :assignedTo)
        AND   (:createdBy IS NULL OR t.createdBy.id = :createdBy)
    """)
    List<Ticket> search(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("assignedTo") Long assignedTo,
            @Param("createdBy") Long createdBy
    );
}
