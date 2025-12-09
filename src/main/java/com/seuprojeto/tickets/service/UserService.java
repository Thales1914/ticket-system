package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // ----------------------------------------------------
    // CRIAR USUARIO
    // ----------------------------------------------------
    public UserResponseDTO createUser(CreateUserDTO dto) {

        if (dto.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Criacao de novos administradores nao e permitida.");
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ja esta em uso.");
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(dto.role())
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
}
