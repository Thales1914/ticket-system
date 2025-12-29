package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateTicketDTO;
import com.seuprojeto.tickets.dto.TicketResponseDTO;
import com.seuprojeto.tickets.entity.Department;
import com.seuprojeto.tickets.entity.Ticket;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.TicketPriority;
import com.seuprojeto.tickets.enums.TicketStatus;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.repository.DepartmentRepository;
import com.seuprojeto.tickets.repository.TicketRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import com.seuprojeto.tickets.specs.TicketSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketHistoryService ticketHistoryService;

    private User findUserOrFail(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }

    private Ticket findTicketOrFail(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket nao encontrado"));
    }

    public Ticket getTicketForUser(Long ticketId, Long requesterId) {
        Ticket ticket = findTicketOrFail(ticketId);
        User requester = findUserOrFail(requesterId);
            ensureCanView(ticket, requester);
        return ticket;
    }

    private Department findDepartmentOrFail(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento nao encontrado"));
    }

    private void ensureCanView(Ticket ticket, User requester) {
        if (requester.getRole() == UserRole.CLIENT) {
            Long creatorId = ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null;
            if (!requester.getId().equals(creatorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao tem permissao para acessar este ticket.");
            }
        } else if (requester.getRole() == UserRole.AGENT) {
            if (!isUserInDepartment(requester, ticket.getDepartment())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agente nao pertence ao departamento deste ticket.");
            }
        }
    }


    public TicketResponseDTO createTicket(CreateTicketDTO dto, Long userId) {
        User creator = findUserOrFail(userId);
        Department department = findDepartmentOrFail(dto.departmentId());

        if (department.getActive() != null && !department.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departamento inativo.");
        }

        Ticket ticket = Ticket.builder()
                .title(dto.title())
                .description(dto.description())
                .priority(dto.priority())
                .status(TicketStatus.ABERTO)
                .department(department)
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


    public TicketResponseDTO getById(Long ticketId, Long requesterId) {
        Ticket ticket = findTicketOrFail(ticketId);
        User requester = findUserOrFail(requesterId);
        ensureCanView(ticket, requester);
        return toDTO(ticket);
    }


    public List<TicketResponseDTO> listAll(Long requesterId) {
        User requester = findUserOrFail(requesterId);

        if (requester.getRole() == UserRole.AGENT) {
            Set<Long> deptIds = requester.getDepartments() != null
                    ? requester.getDepartments().stream().map(Department::getId).collect(Collectors.toSet())
                    : Set.of();

            if (deptIds.isEmpty()) {
                return List.of();
            }

            Specification<Ticket> spec = Specification.where(TicketSpecifications.departmentIn(deptIds));
            List<Ticket> tickets = ticketRepository.findAll(spec);
            return toDTOList(tickets);
        }

        List<Ticket> tickets = ticketRepository.findAll();
        return toDTOList(tickets);
    }

    public List<TicketResponseDTO> listByUser(Long userId) {
        List<Ticket> tickets = ticketRepository.findByCreatedById(userId);
        return toDTOList(tickets);
    }


    public TicketResponseDTO updateStatus(Long ticketId, TicketStatus newStatus, Long userId) {
        Ticket ticket = findTicketOrFail(ticketId);
        User user = findUserOrFail(userId);

        TicketStatus current = ticket.getStatus();

        if (user.getRole() == UserRole.AGENT && !isUserInDepartment(user, ticket.getDepartment())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agente nao pertence ao departamento deste ticket.");
        }

        if (current == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ticket ja esta com o status " + newStatus);
        }

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


    public TicketResponseDTO assignTicket(Long ticketId, Long agentId, Long requesterId) {
        Ticket ticket = findTicketOrFail(ticketId);
        User requester = findUserOrFail(requesterId);
        User agent = findUserOrFail(agentId);

        if (ticket.getStatus() == TicketStatus.RESOLVIDO || ticket.getStatus() == TicketStatus.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket ja finalizado nao pode ser atribuido.");
        }

        if (agent.getRole() == UserRole.CLIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente AGENT ou ADMIN podem ser atribuidos como responsaveis.");
        }

        if (requester.getRole() == UserRole.CLIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clientes nao podem atribuir tickets.");
        }

        if (requester.getRole() == UserRole.AGENT && !requesterId.equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agentes so podem atribuir tickets para si mesmos.");
        }

        Department ticketDepartment = ticket.getDepartment();
        if (ticketDepartment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket sem departamento configurado.");
        }

        if (requester.getRole() == UserRole.AGENT && !isUserInDepartment(requester, ticketDepartment)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agente nao pertence ao departamento deste ticket.");
        }

        if (agent.getRole() == UserRole.AGENT && !isUserInDepartment(agent, ticketDepartment)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsavel escolhido nao pertence ao departamento do ticket.");
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

    public TicketResponseDTO updateDepartment(Long ticketId, Long departmentId, Long requesterId) {
        Ticket ticket = findTicketOrFail(ticketId);
        User requester = findUserOrFail(requesterId);

        if (requester.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente ADMIN pode mudar o departamento do ticket.");
        }

        Department newDepartment = findDepartmentOrFail(departmentId);
        Department currentDepartment = ticket.getDepartment();

        if (currentDepartment != null && currentDepartment.getId().equals(newDepartment.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket ja pertence a este departamento.");
        }

        if (newDepartment.getActive() != null && !newDepartment.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departamento inativo.");
        }

        ticket.setDepartment(newDepartment);
        ticket.setAssignedTo(null);
        ticket.setStatus(TicketStatus.ABERTO);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

        ticketHistoryService.log(
                saved,
                "DEPARTMENT_CHANGED",
                currentDepartment != null ? currentDepartment.getName() : null,
                newDepartment.getName(),
                requester.getId()
        );

        return toDTO(saved);
    }

    @Transactional
    public TicketResponseDTO assignNextAvailable(Long agentId) {
        User agent = findUserOrFail(agentId);
        if (agent.getRole() != UserRole.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas agentes podem puxar tickets do seu setor.");
        }

        Set<Department> departments = agent.getDepartments();
        if (departments == null || departments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agente nao possui setores vinculados.");
        }

        List<Long> deptIds = departments.stream().map(Department::getId).toList();
        Ticket ticket = ticketRepository.findNextAvailableForDepartments(deptIds, TicketStatus.ABERTO);

        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum ticket disponivel para seus setores.");
        }

        if (ticket.getAssignedTo() != null || ticket.getStatus() != TicketStatus.ABERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket acabou de ser atribuido. Tente novamente.");
        }

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.EM_ATENDIMENTO);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

        ticketHistoryService.log(
                saved,
                "ASSIGNED_AUTO",
                null,
                agent.getName(),
                agent.getId()
        );

        return toDTO(saved);
    }


    public List<TicketResponseDTO> searchTickets(
            TicketStatus status,
            TicketPriority priority,
            Long createdBy,
            Long assignedTo,
            Long departmentId,
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

        if (requester.getRole() == UserRole.AGENT) {
            Set<Long> userDepartments = requester.getDepartments() != null
                    ? requester.getDepartments().stream().map(Department::getId).collect(Collectors.toSet())
                    : Set.of();

            if (userDepartments.isEmpty()) {
                return List.of();
            }

            if (departmentId != null && !userDepartments.contains(departmentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agente nao pertence ao departamento solicitado.");
            }

            spec = spec.and(TicketSpecifications.departmentIn(
                    departmentId != null ? Set.of(departmentId) : userDepartments
            ));
        } else {
            spec = spec.and(TicketSpecifications.department(departmentId));
        }

        List<Ticket> result = ticketRepository.findAll(spec);
        return toDTOList(result);
    }


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
                saved.getDepartment() != null ? saved.getDepartment().getId() : null,
                saved.getDepartment() != null ? saved.getDepartment().getName() : null,
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

    private boolean isUserInDepartment(User user, Department department) {
        Set<Department> departments = user.getDepartments();
        if (departments == null || department == null) return false;
        return departments.stream().anyMatch(d -> d.getId().equals(department.getId()));
    }
}
