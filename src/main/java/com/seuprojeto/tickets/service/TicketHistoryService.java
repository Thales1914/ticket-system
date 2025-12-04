package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.TicketHistory;
import com.seuprojeto.tickets.repository.TicketHistoryRepository;
import com.seuprojeto.tickets.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private final TicketHistoryRepository historyRepository;
    private final TicketRepository ticketRepository;

    public void log(Ticket ticket, String action, String oldVal, String newVal, Long performedBy) {

        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .action(action)
                .oldValue(oldVal)
                .newValue(newVal)
                .performedBy(performedBy)
                .timestamp(LocalDateTime.now())
                .build();

        historyRepository.save(history);
    }

    public List<TicketHistory> getHistoryForTicket(Long ticketId) {
        return historyRepository.findByTicketIdOrderByTimestampDesc(ticketId);
    }
}
