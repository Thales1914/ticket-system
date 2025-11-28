package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping
@RestController
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(
            @Valid @RequestBody CreateTicketDTO dto
            ){
        Long fakeUserId = 1L;
        TicketResponseDTO ticket = ticketService.createTicket(dto, fakeUserId);

        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> listTickets() {
        return ResponseEntity.ok(ticketService.listAll());
    }

}
