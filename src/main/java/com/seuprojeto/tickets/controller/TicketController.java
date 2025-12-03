package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.dto.UpdateStatusDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(
            @Valid @RequestBody CreateTicketDTO dto
    ) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        TicketResponseDTO ticket = ticketService.createTicket(dto, userId);

        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> listTickets() {
        return ResponseEntity.ok(ticketService.listAll());
    }

    @GetMapping("/me")
    public ResponseEntity<List<Ticket>> listMyTickets() {

        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        return ResponseEntity.ok(ticketService.listByUser(userId));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponseDTO> updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateStatusDTO dto
    ) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        TicketResponseDTO updated = ticketService.updateStatus(ticketId, dto.status(), userId);

        return ResponseEntity.ok(updated);
    }
}
