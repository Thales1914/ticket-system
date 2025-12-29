package com.seuprojeto.tickets.service;

import com.seuprojeto.tickets.dto.DepartmentDTO;
import com.seuprojeto.tickets.entity.Department;
import com.seuprojeto.tickets.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public Department findByIdOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento nao encontrado."));
    }

    public List<DepartmentDTO> listAll() {
        return departmentRepository.findAll()
                .stream()
                .map(DepartmentDTO::new)
                .toList();
    }

    public List<DepartmentDTO> listActive() {
        return departmentRepository.findAll()
                .stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .map(DepartmentDTO::new)
                .toList();
    }

    public DepartmentDTO create(String name, String description, boolean active) {
        departmentRepository.findByNameIgnoreCase(name)
                .ifPresent(d -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Departamento ja existe.");
                });

        Department saved = departmentRepository.save(
                Department.builder()
                        .name(name)
                        .description(description)
                        .active(active)
                        .build()
        );
        return new DepartmentDTO(saved);
    }
}
