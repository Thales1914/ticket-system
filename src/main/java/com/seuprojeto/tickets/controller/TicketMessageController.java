package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateMessageDTO;
import com.seuprojeto.tickets.dto.MessageResponseDTO;
import com.seuprojeto.tickets.entity.TicketMessage;
import com.seuprojeto.tickets.service.TicketMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class TicketMessageController {

    private final TicketMessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @Valid @RequestBody CreateMessageDTO dto
    ) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        return ResponseEntity.ok(messageService.sendMessage(dto, userId));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<List<TicketMessage>> getMessages(
            @PathVariable Long ticketId
    ) {
        return ResponseEntity.ok(messageService.getMessages(ticketId));
    }
}
