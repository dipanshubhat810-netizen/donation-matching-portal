package com.sevasahayog.donationmatching.config;

import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${admin.email:}") String adminEmail,
                       @Value("${admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            if (adminPassword != null && !adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD is set but ADMIN_EMAIL is not. Set both ADMIN_EMAIL and ADMIN_PASSWORD to seed an admin account.");
            }
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL is set but ADMIN_PASSWORD is not. Set both ADMIN_EMAIL and ADMIN_PASSWORD to seed an admin account.");
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least 8 characters long for the seeded admin account.");
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return;
        }

        userRepository.save(User.builder()
                .name("Administrator")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .build());
        log.info("Seeded admin account for {}", normalizedEmail);
    }
}
