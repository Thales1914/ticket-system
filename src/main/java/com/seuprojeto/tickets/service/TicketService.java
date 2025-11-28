package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.repository.TicketRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketResponseDTO createTicket(CreateTicketDTO dto, Long userId) {

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Ticket ticket = Ticket.builder()
                .title(dto.title())
                .description(dto.description())
                .priority(dto.priority())
                .status(TicketStatus.ABERTO)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Ticket saved = ticketRepository.save(ticket);

        return new TicketResponseDTO(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getPriority(),
                saved.getStatus(),
                saved.getCreatedBy().getId(),
                saved.getAssignedTo() != null ? saved.getAssignedTo().getId() : null,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    public List<Ticket> listAll() {
        return ticketRepository.findAll();
    }
}
