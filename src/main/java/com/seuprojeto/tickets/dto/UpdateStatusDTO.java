package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusDTO(
        @NotNull TicketStatus status
) {}
