package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponseDTO(
        Long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Long createdById,
        String createdByName,
        String createdByEmail,
        Long assignedToId,
        String assignedToName,
        String assignedToEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
