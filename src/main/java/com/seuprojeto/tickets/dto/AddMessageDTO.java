package com.seuprojeto.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddMessageDTO(
        @NotBlank
        @Size(max = 1000)
        String content
) {}
