package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.enums.UserRole;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        UserRole role
) {}
