package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody CreateUserDTO dto
    ){
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @GetMapping
    public ResponseEntity<List<User>> listAllUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }
}
