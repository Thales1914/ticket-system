package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.DepartmentDTO;
import com.seuprojeto.tickets.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DepartmentDTO>> listAll() {
        return ResponseEntity.ok(departmentService.listAll());
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentDTO>> listActive() {
        return ResponseEntity.ok(departmentService.listActive());
    }
}
