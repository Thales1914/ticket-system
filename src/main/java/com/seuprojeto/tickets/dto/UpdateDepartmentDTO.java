package com.seuprojeto.tickets.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDepartmentDTO(
        @NotNull Long departmentId
) {}
