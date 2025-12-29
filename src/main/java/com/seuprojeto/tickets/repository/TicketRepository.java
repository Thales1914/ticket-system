package com.seuprojeto.tickets.repository;

import com.seuprojeto.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByCreatedById(Long userId);

    long countByCreatedById(Long userId);

    long countByAssignedToId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Ticket t
            where t.department.id in :departmentIds
              and t.status = :status
              and t.assignedTo is null
            order by t.createdAt asc
            """)
    Ticket findNextAvailableForDepartments(List<Long> departmentIds, com.seuprojeto.tickets.enums.TicketStatus status);
}
