package com.seuprojeto.tickets.config;

import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // ADMIN
        createUserIfNotExists(
                "Admin Teste",
                "admin@test.com",
                "123456",
                UserRole.ADMIN
        );

        // AGENT
        createUserIfNotExists(
                "Agente Teste",
                "agente@test.com",
                "123456",
                UserRole.AGENT
        );

        // CLIENT
        createUserIfNotExists(
                "Cliente Teste",
                "cliente@test.com",
                "123456",
                UserRole.CLIENT
        );
    }

    private void createUserIfNotExists(String name, String email, String rawPassword, UserRole role) {
        userRepository.findByEmail(email).ifPresentOrElse(
                u -> {},
                () -> {
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    userRepository.save(user);
                    System.out.println("[DataInitializer] Usuário criado: " + email + " (" + role + ")");
                }
        );
    }
}
