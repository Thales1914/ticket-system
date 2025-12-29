package com.seuprojeto.tickets.config;

import com.seuprojeto.tickets.entity.Department;
import com.seuprojeto.tickets.entity.User;
import com.seuprojeto.tickets.enums.UserRole;
import com.seuprojeto.tickets.repository.DepartmentRepository;
import com.seuprojeto.tickets.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;
    @Value("${app.seed.default-password:ChangeMe123!}")
    private String defaultPassword;

    @Override
    public void run(String... args) throws Exception {

        if (!seedEnabled) {
            return;
        }

        Department financeiro = createDepartmentIfNotExists("Financeiro", "Demandas financeiras", true);
        Department rh = createDepartmentIfNotExists("RH", "Recursos Humanos", true);
        Department comercial = createDepartmentIfNotExists("Comercial", "Atendimento ao time comercial", true);

        // ADMIN apenas
        createUserIfNotExists("Admin Teste", "admin@test.com", defaultPassword, UserRole.ADMIN, Set.of(financeiro, rh, comercial));
    }

    private Department createDepartmentIfNotExists(String name, String description, boolean active) {
        return departmentRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Department department = Department.builder()
                    .name(name)
                    .description(description)
                    .active(active)
                    .build();
            Department saved = departmentRepository.save(department);
            System.out.println("[DataInitializer] Departamento criado: " + name);
            return saved;
        });
    }

    private void createUserIfNotExists(String name, String email, String rawPassword, UserRole role, Set<Department> departments) {
        userRepository.findByEmail(email).ifPresentOrElse(
                u -> {},
                () -> {
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    user.setDepartments(new HashSet<>(departments));
                    userRepository.save(user);
                    System.out.println("[DataInitializer] Usuario criado: " + email + " (" + role + ")");
                }
        );
    }
}
