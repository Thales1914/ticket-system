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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;

    private User findUserOrFail(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }

    private Ticket findTicketOrFail(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket nao encontrado"));
    }

    private void ensureCanView(Ticket ticket, User requester) {
        if (requester.getRole() == UserRole.CLIENT) {
            Long creatorId = ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null;
            if (!requester.getId().equals(creatorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao tem permissao para acessar este ticket.");
            }
        }
    }

    // ---------------------- CREATE ----------------------

    public TicketResponseDTO createTicket(CreateTicketDTO dto, Long userId) {
        User creator = findUserOrFail(userId);

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

    public TicketResponseDTO getById(Long ticketId, Long requesterId) {
        Ticket ticket = findTicketOrFail(ticketId);
        User requester = findUserOrFail(requesterId);
        ensureCanView(ticket, requester);
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
        Ticket ticket = findTicketOrFail(ticketId);
        User user = findUserOrFail(userId);

        TicketStatus current = ticket.getStatus();

        if (current == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ticket ja esta com o status " + newStatus);
        }

        // fluxo permitido
        switch (current) {
            case ABERTO -> {
                if (newStatus != TicketStatus.EM_ATENDIMENTO &&
                        newStatus != TicketStatus.CANCELADO) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ABERTO so pode ir para EM_ATENDIMENTO ou CANCELADO.");
                }
            }
            case EM_ATENDIMENTO -> {
                if (newStatus != TicketStatus.RESOLVIDO &&
                        newStatus != TicketStatus.CANCELADO) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EM_ATENDIMENTO so pode ir para RESOLVIDO ou CANCELADO.");
                }
            }
            case RESOLVIDO, CANCELADO -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao e possivel alterar um ticket RESOLVIDO ou CANCELADO.");
        }

        // regras por role
        switch (newStatus) {
            case EM_ATENDIMENTO, RESOLVIDO -> {
                if (user.getRole() == UserRole.CLIENT) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente AGENT ou ADMIN podem fazer essa acao.");
                }
            }
            case CANCELADO -> {
                if (user.getRole() != UserRole.ADMIN) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente ADMIN pode cancelar tickets.");
                }
            }
        }

        TicketStatus oldStatus = current;
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

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
        Ticket ticket = findTicketOrFail(ticketId);
        User requester = findUserOrFail(requesterId);
        User agent = findUserOrFail(agentId);

        if (agent.getRole() == UserRole.CLIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente AGENT ou ADMIN podem ser atribuidos como responsaveis.");
        }

        if (requester.getRole() == UserRole.CLIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clientes nao podem atribuir tickets.");
        }

        if (requester.getRole() == UserRole.AGENT && !requesterId.equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agentes so podem atribuir tickets para si mesmos.");
        }

        if (ticket.getAssignedTo() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este ticket ja foi atribuido.");
        }

        User oldAssignee = ticket.getAssignedTo();

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.EM_ATENDIMENTO);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

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
            LocalDate to,
            Long requesterId
    ) {
        User requester = findUserOrFail(requesterId);

        Long createdFilter = createdBy;
        if (requester.getRole() == UserRole.CLIENT) {
            createdFilter = requesterId;
        }

        Specification<Ticket> spec = Specification.where(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.createdBy(createdFilter))
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
                saved.getCreatedBy() != null ? saved.getCreatedBy().getEmail() : null,
                saved.getAssignedTo() != null ? saved.getAssignedTo().getId() : null,
                saved.getAssignedTo() != null ? saved.getAssignedTo().getName() : null,
                saved.getAssignedTo() != null ? saved.getAssignedTo().getEmail() : null,
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
