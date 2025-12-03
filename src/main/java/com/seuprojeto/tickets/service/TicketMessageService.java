package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateMessageDTO;
import com.seuprojeto.tickets.dto.MessageResponseDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.TicketMessage;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.repository.TicketMessageRepository;
import com.seuprojeto.tickets.repository.TicketRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketMessageService {

    private final TicketMessageRepository messageRepo;
    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;

    public MessageResponseDTO sendMessage(CreateMessageDTO dto, Long userId) {

        Ticket ticket = ticketRepo.findById(dto.ticketId())
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));

        User author = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .author(author)
                .message(dto.message())
                .createdAt(LocalDateTime.now())
                .build();

        TicketMessage saved = messageRepo.save(msg);

        return new MessageResponseDTO(
                saved.getId(),
                saved.getTicket().getId(),
                saved.getAuthor().getId(),
                saved.getMessage(),
                saved.getCreatedAt()
        );
    }

    public List<TicketMessage> getMessages(Long ticketId) {
        return messageRepo.findAll()
                .stream()
                .filter(m -> m.getTicket().getId().equals(ticketId))
                .toList();
    }
}
