package com.seuprojeto.tickets.controller;

import com.seuprojeto.tickets.dto.CreateUserDTO;
import com.seuprojeto.tickets.dto.LoginDTO;
import com.seuprojeto.tickets.dto.LoginResponseDTO;
import com.seuprojeto.tickets.dto.UserResponseDTO;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.security.JwtService;
import com.seuprojeto.tickets.service.UserService;
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
    public UserResponseDTO register(@RequestBody CreateUserDTO dto) {
        return userService.createUser(dto);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginDTO dto) {

        var authToken = new UsernamePasswordAuthenticationToken(
                dto.email(),
                dto.password()
        );

        authenticationManager.authenticate(authToken);

        User user = userService.getUserByEmail(dto.email());

        String token = jwtService.generateToken(
                user.getId(),
                user.getRole().name()
        );

        return new LoginResponseDTO(token);
    }
}
