package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.dto.UpdateStatusDTO;
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

    // ---------------------- CREATE ----------------------

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody CreateTicketDTO dto) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.createTicket(dto, userId));
    }

    // ---------------------- LIST ----------------------

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> listTickets() {
        return ResponseEntity.ok(ticketService.listAll());
    }

    @GetMapping("/me")
    public ResponseEntity<List<TicketResponseDTO>> listMyTickets() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.listByUser(userId));
    }

    // ---------------------- GET BY ID (DETALHES) ----------------------

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long ticketId) {
        TicketResponseDTO dto = ticketService.getById(ticketId);
        return ResponseEntity.ok(dto);
    }

    // ---------------------- STATUS UPDATE ----------------------

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponseDTO> updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateStatusDTO dto
    ) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.updateStatus(ticketId, dto.status(), userId));
    }

    // ---------------------- ASSIGN ----------------------

    @PatchMapping("/{ticketId}/assign/{agentId}")
    public ResponseEntity<TicketResponseDTO> assignTicket(
            @PathVariable Long ticketId,
            @PathVariable Long agentId
    ) {
        Long requesterId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, agentId, requesterId));
    }

    // "Assumir pra mim" (AGENT/ADMIN)
    @PatchMapping("/{ticketId}/assign/me")
    public ResponseEntity<TicketResponseDTO> assignToCurrentUser(
            @PathVariable Long ticketId
    ) {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, currentUserId, currentUserId));
    }

    // ---------------------- SEARCH ----------------------

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponseDTO>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {

        List<TicketResponseDTO> result = ticketService.searchTickets(
                status, priority, createdBy, assignedTo, from, to
        );
        return ResponseEntity.ok(result);
    }
}
