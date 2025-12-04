package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;
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
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

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

    public List<Ticket> listByUser(Long userId) {
        return ticketRepository.findByCreatedById(userId);
    }

    public TicketResponseDTO updateStatus(Long ticketId, TicketStatus newStatus, Long userId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        TicketStatus current = ticket.getStatus();

        if (current == newStatus)
            throw new RuntimeException("O ticket já está com o status " + newStatus);

        switch (current) {

            case ABERTO -> {
                if (newStatus != TicketStatus.EM_ATENDIMENTO &&
                        newStatus != TicketStatus.CANCELADO)
                    throw new RuntimeException("ABERTO só pode ir para EM_ATENDIMENTO ou CANCELADO.");
            }

            case EM_ATENDIMENTO -> {
                if (newStatus != TicketStatus.RESOLVIDO &&
                        newStatus != TicketStatus.CANCELADO)
                    throw new RuntimeException("EM_ATENDIMENTO só pode ir para RESOLVIDO ou CANCELADO.");
            }

            case RESOLVIDO, CANCELADO -> {
                throw new RuntimeException("Não é possível alterar um ticket que está RESOLVIDO ou CANCELADO.");
            }
        }

        switch (newStatus) {

            case EM_ATENDIMENTO -> {
                if (user.getRole() == UserRole.CLIENT)
                    throw new RuntimeException("Somente AGENT ou ADMIN podem iniciar atendimento.");
            }

            case RESOLVIDO -> {
                if (user.getRole() == UserRole.CLIENT)
                    throw new RuntimeException("Somente AGENT ou ADMIN podem resolver tickets.");
            }

            case CANCELADO -> {
                if (user.getRole() != UserRole.ADMIN)
                    throw new RuntimeException("Somente ADMIN pode cancelar tickets.");
            }
        }

        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

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

    public TicketResponseDTO assignTicket(Long ticketId, Long agentId, Long requesterId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado."));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Usuário solicitante não encontrado."));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agente não encontrado."));

        if (requester.getRole() == UserRole.CLIENT) {
            throw new RuntimeException("Clientes não podem atribuir tickets.");
        }

        if (requester.getRole() == UserRole.AGENT && !requesterId.equals(agentId)) {
            throw new RuntimeException("Agentes só podem atribuir tickets para si mesmos.");
        }

        if (ticket.getStatus() != TicketStatus.ABERTO) {
            throw new RuntimeException("Somente tickets ABERTOS podem ser atribuídos.");
        }

        if (ticket.getAssignedTo() != null) {
            throw new RuntimeException("Este ticket já foi atribuído.");
        }

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.EM_ATENDIMENTO);
        ticket.setUpdatedAt(LocalDateTime.now());

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

    public List<Ticket> searchTickets(
            TicketStatus status,
            TicketPriority priority,
            Long assignedTo,
            Long createdBy
    ) {
        return ticketRepository.search(status, priority, assignedTo, createdBy);
    }
}
