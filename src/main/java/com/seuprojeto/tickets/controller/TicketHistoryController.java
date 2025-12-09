package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.TicketHistoryDTO;
import com.seuprojeto.tickets.entity.TicketHistory;
import com.seuprojeto.tickets.repository.UserRepository;
import com.seuprojeto.tickets.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketHistoryController {

    private final TicketHistoryService ticketHistoryService;
    private final UserRepository userRepository;

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<List<TicketHistoryDTO>> getHistory(@PathVariable Long ticketId) {
        List<TicketHistory> history = ticketHistoryService.getHistoryForTicket(ticketId);

        List<TicketHistoryDTO> dtoList = history.stream()
                .map(h -> {
                    var user = userRepository.findById(h.getPerformedBy()).orElse(null);
                    String name = user != null ? user.getName() : null;
                    String email = user != null ? user.getEmail() : null;
                    return TicketHistoryDTO.fromEntity(h, name, email);
                })
                .toList();

        return ResponseEntity.ok(dtoList);
    }
}
    