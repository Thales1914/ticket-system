package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.LoginDTO;
import com.seuprojeto.tickets.dto.LoginResponseDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.security.JwtService;
import com.seuprojeto.tickets.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/register")
    public UserResponseDTO register(@Valid @RequestBody CreateUserDTO dto) {
        CreateUserDTO safeDto = new CreateUserDTO(
                dto.name(),
                dto.email(),
                dto.password(),
                UserRole.CLIENT,
                dto.departmentIds()
        );

        return userService.createUser(safeDto);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginDTO dto) {

        var authToken = new UsernamePasswordAuthenticationToken(
                dto.email(),
                dto.password()
        );

        authenticationManager.authenticate(authToken);

        User user = userService.getUserByEmail(dto.email());

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        UserResponseDTO userDto = new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponseDTO(token, userDto);
    }
}
