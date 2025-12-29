package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.repository.DepartmentRepository;
import com.seuprojeto.tickets.repository.TicketRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketRepository ticketRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserResponseDTO createUser(CreateUserDTO dto) {

        if (dto.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Criacao de novos administradores nao e permitida.");
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ja esta em uso.");
        }

        Set<com.seuprojeto.tickets.entity.Department> departments = new HashSet<>();
        if (dto.departmentIds() != null) {
            dto.departmentIds().forEach(id -> {
                com.seuprojeto.tickets.entity.Department dept = departmentRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento nao encontrado: " + id));
                departments.add(dept);
            });
        }

        if (dto.role() == UserRole.AGENT && departments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agente deve estar vinculado a pelo menos um departamento.");
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(dto.role())
                .departments(departments)
                .build();

        User saved = userRepository.save(user);

        return new UserResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole()
        );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }

    public List<UserResponseDTO> listAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<UserResponseDTO> listAgentsByDepartment(Long departmentId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.AGENT)
                .filter(u -> departmentId == null || (u.getDepartments() != null && u.getDepartments().stream().anyMatch(d -> d.getId().equals(departmentId))))
                .map(UserResponseDTO::new)
                .toList();
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Remover administradores nao e permitido.");
        }

        long createdCount = ticketRepository.countByCreatedById(userId);
        long assignedCount = ticketRepository.countByAssignedToId(userId);
        if (createdCount > 0 || assignedCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao e possivel remover usuario com tickets vinculados.");
        }

        userRepository.delete(user);
    }
}
