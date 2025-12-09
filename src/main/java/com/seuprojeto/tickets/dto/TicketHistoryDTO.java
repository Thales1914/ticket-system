package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.entity.TicketHistory;
import java.time.LocalDateTime;

public record TicketHistoryDTO(
        Long id,
        String action,
        String oldValue,
        String newValue,
        Long performedBy,
        String performedByName,
        String performedByEmail,
        LocalDateTime timestamp
) {
    public static TicketHistoryDTO fromEntity(TicketHistory h, String name, String email) {
        return new TicketHistoryDTO(
                h.getId(),
                h.getAction(),
                h.getOldValue(),
                h.getNewValue(),
                h.getPerformedBy(),
                name,
                email,
                h.getTimestamp()
        );
    }
}
