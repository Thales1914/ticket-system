package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.repository.TicketRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import com.seuprojeto.tickets.specs.TicketSpecifications;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;

    // ---------------------- CREATE ----------------------

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

        // histórico: CREATED
        ticketHistoryService.log(
                saved,
                "CREATED",
                null,
                null,
                creator.getId()
        );

        return toDTO(saved);
    }

    // ---------------------- GET BY ID ----------------------

    public TicketResponseDTO getById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));
        return toDTO(ticket);
    }

    // ---------------------- LIST ----------------------

    public List<TicketResponseDTO> listAll() {
        List<Ticket> tickets = ticketRepository.findAll();
        return toDTOList(tickets);
    }

    public List<TicketResponseDTO> listByUser(Long userId) {
        List<Ticket> tickets = ticketRepository.findByCreatedById(userId);
        return toDTOList(tickets);
    }

    // ---------------------- STATUS UPDATE ----------------------

    public TicketResponseDTO updateStatus(Long ticketId, TicketStatus newStatus, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        TicketStatus current = ticket.getStatus();

        if (current == newStatus)
            throw new RuntimeException("O ticket já está com o status " + newStatus);

        // regras de fluxo
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

        // regras por role
        switch (newStatus) {
            case EM_ATENDIMENTO, RESOLVIDO -> {
                if (user.getRole() == UserRole.CLIENT)
                    throw new RuntimeException("Somente AGENT ou ADMIN podem fazer essa ação.");
            }
            case CANCELADO -> {
                if (user.getRole() != UserRole.ADMIN)
                    throw new RuntimeException("Somente ADMIN pode cancelar tickets.");
            }
        }

        TicketStatus oldStatus = current;
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

        // histórico: STATUS_CHANGED
        ticketHistoryService.log(
                saved,
                "STATUS_CHANGED",
                oldStatus.name(),
                newStatus.name(),
                user.getId()
        );

        return toDTO(saved);
    }

    // ---------------------- ASSIGN ----------------------

    public TicketResponseDTO assignTicket(Long ticketId, Long agentId, Long requesterId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket não encontrado."));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Usuário solicitante não encontrado."));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agente não encontrado."));

        if (requester.getRole() == UserRole.CLIENT)
            throw new RuntimeException("Clientes não podem atribuir tickets.");

        if (requester.getRole() == UserRole.AGENT && !requesterId.equals(agentId))
            throw new RuntimeException("Agentes só podem atribuir tickets para si mesmos.");

        if (ticket.getAssignedTo() != null)
            throw new RuntimeException("Este ticket já foi atribuído.");

        User oldAssignee = ticket.getAssignedTo();

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.EM_ATENDIMENTO);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

        // histórico: ASSIGNED
        ticketHistoryService.log(
                saved,
                "ASSIGNED",
                oldAssignee != null ? oldAssignee.getName() : null,
                agent.getName(),
                requester.getId()
        );

        return toDTO(saved);
    }

    // ---------------------- SEARCH ----------------------

    public List<TicketResponseDTO> searchTickets(
            TicketStatus status,
            TicketPriority priority,
            Long createdBy,
            Long assignedTo,
            LocalDate from,
            LocalDate to
    ) {

        Specification<Ticket> spec = Specification.where(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.createdBy(createdBy))
                .and(TicketSpecifications.assignedTo(assignedTo))
                .and(TicketSpecifications.createdAfter(from != null ? from.atStartOfDay() : null))
                .and(TicketSpecifications.createdBefore(to != null ? to.atTime(23, 59, 59) : null));

        List<Ticket> result = ticketRepository.findAll(spec);
        return toDTOList(result);
    }

    // ---------------------- HELPERS ----------------------

    private TicketResponseDTO toDTO(Ticket saved) {
        return new TicketResponseDTO(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getPriority(),
                saved.getStatus(),
                saved.getCreatedBy() != null ? saved.getCreatedBy().getId() : null,
                saved.getCreatedBy() != null ? saved.getCreatedBy().getName() : null,
                saved.getAssignedTo() != null ? saved.getAssignedTo().getId() : null,
                saved.getAssignedTo() != null ? saved.getAssignedTo().getName() : null,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private List<TicketResponseDTO> toDTOList(List<Ticket> tickets) {
        return tickets.stream()
                .map(this::toDTO)
                .toList();
    }
}
