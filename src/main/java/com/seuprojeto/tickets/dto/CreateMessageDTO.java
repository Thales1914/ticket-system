package com.seuprojeto.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMessageDTO(
        @NotNull Long ticketId,
        @NotBlank String message
) {}
