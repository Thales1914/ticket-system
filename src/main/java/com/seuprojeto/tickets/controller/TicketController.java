package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.dto.UpdateStatusDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    private Long getAuthenticatedUserId() {
        String userIdString = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return Long.parseLong(userIdString);
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody CreateTicketDTO dto) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.createTicket(dto, userId));
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> listTickets() {
        return ResponseEntity.ok(ticketService.listAll());
    }

    @GetMapping("/me")
    public ResponseEntity<List<Ticket>> listMyTickets() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.listByUser(userId));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponseDTO> updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateStatusDTO dto
    ) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.updateStatus(ticketId, dto.status(), userId));
    }

    @PatchMapping("/{ticketId}/assign/{agentId}")
    public ResponseEntity<TicketResponseDTO> assignTicket(
            @PathVariable Long ticketId,
            @PathVariable Long agentId
    ) {
        Long requesterId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, agentId, requesterId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Ticket>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {

        List<Ticket> result = ticketService.searchTickets(status, priority, createdBy, assignedTo, from, to);
        return ResponseEntity.ok(result);
    }
}
