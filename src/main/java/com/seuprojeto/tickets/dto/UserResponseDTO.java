package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        UserRole role
) {
    public UserResponseDTO(User user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
