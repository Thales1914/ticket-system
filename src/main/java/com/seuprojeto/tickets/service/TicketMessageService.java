package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.TicketMessageDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.TicketMessage;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.repository.TicketMessageRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketMessageService {

    private final TicketMessageRepository messageRepository;
    private final TicketService ticketService;
    private final UserRepository userRepository;

    public List<TicketMessageDTO> listMessages(Long ticketId, Long requesterId) {
        ticketService.getTicketForUser(ticketId, requesterId);
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(TicketMessageDTO::new)
                .toList();
    }

    public TicketMessageDTO addMessage(Long ticketId, Long requesterId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem vazia nao permitida.");
        }

        Ticket ticket = ticketService.getTicketForUser(ticketId, requesterId);
        if (ticket.getStatus() == TicketStatus.CANCELADO || ticket.getStatus() == TicketStatus.RESOLVIDO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket finalizado nao aceita mensagens.");
        }

        User author = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        TicketMessage saved = messageRepository.save(
                TicketMessage.builder()
                        .ticket(ticket)
                        .authorId(author.getId())
                        .authorName(author.getName())
                        .authorEmail(author.getEmail())
                        .content(content.trim())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return new TicketMessageDTO(saved);
    }
}
