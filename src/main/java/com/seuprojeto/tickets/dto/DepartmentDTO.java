package com.seuprojeto.tickets.dto;

import com.seuprojeto.tickets.entity.Department;

public record DepartmentDTO(
        Long id,
        String name,
        String description,
        Boolean active
) {
    public DepartmentDTO(Department d) {
        this(d.getId(), d.getName(), d.getDescription(), d.getActive());
    }
}
