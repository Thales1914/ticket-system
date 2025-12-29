package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.entity.TicketMessage;

import java.time.LocalDateTime;

public record TicketMessageDTO(
        Long id,
        Long authorId,
        String authorName,
        String authorEmail,
        String content,
        LocalDateTime createdAt
) {
    public TicketMessageDTO(TicketMessage message) {
        this(
                message.getId(),
                message.getAuthorId(),
                message.getAuthorName(),
                message.getAuthorEmail(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
