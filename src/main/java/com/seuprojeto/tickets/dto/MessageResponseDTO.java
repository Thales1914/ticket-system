package com.seuprojeto.tickets.dto;

import java.time.LocalDateTime;

public record MessageResponseDTO(
        Long id,
        Long ticketId,
        Long authorId,
        String message,
        LocalDateTime createdAt
) {}
