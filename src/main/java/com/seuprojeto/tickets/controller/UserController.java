package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody CreateUserDTO dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> listAllUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<UserResponseDTO>> listAgents() {
        List<UserResponseDTO> agents = userService.listAllUsers().stream()
                .filter(u -> u.role() == UserRole.AGENT)
                .toList();
        return ResponseEntity.ok(agents);
    }
}
