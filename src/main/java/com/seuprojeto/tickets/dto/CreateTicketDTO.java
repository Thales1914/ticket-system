package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketDTO(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull TicketPriority priority
) {}
