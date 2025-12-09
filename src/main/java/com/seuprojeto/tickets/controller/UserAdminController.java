package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Já existe usuário com esse e-mail.");
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponseDTO(user));
    }
}
