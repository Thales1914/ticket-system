package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.dto.UpdateDepartmentDTO;
import com.seuprojeto.tickets.dto.UpdateStatusDTO;
import com.seuprojeto.tickets.dto.AddMessageDTO;
import com.seuprojeto.tickets.dto.TicketMessageDTO;
import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.service.TicketService;
import com.seuprojeto.tickets.service.TicketMessageService;

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
    private final TicketMessageService ticketMessageService;

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
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.listAll(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<TicketResponseDTO>> listMyTickets() {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.listByUser(userId));
    }

    // ---------------------- GET BY ID (DETALHES) ----------------------

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long ticketId) {
        Long userId = getAuthenticatedUserId();
        TicketResponseDTO dto = ticketService.getById(ticketId, userId);
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

    @PatchMapping("/assign/next")
    public ResponseEntity<TicketResponseDTO> assignNextForCurrentAgent() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.assignNextAvailable(currentUserId));
    }

    // ---------------------- SEARCH ----------------------

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponseDTO>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {

        Long userId = getAuthenticatedUserId();

        List<TicketResponseDTO> result = ticketService.searchTickets(
                status, priority, createdBy, assignedTo, departmentId, from, to, userId
        );
        return ResponseEntity.ok(result);
    }

    // ---------------------- DEPARTMENT ----------------------

    @PatchMapping("/{ticketId}/department")
    public ResponseEntity<TicketResponseDTO> updateDepartment(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateDepartmentDTO dto
    ) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketService.updateDepartment(ticketId, dto.departmentId(), userId));
    }

    // ---------------------- MESSAGES ----------------------

    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<List<TicketMessageDTO>> listMessages(@PathVariable Long ticketId) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketMessageService.listMessages(ticketId, userId));
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<TicketMessageDTO> addMessage(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddMessageDTO dto
    ) {
        Long userId = getAuthenticatedUserId();
        return ResponseEntity.ok(ticketMessageService.addMessage(ticketId, userId, dto.content()));
    }
}
