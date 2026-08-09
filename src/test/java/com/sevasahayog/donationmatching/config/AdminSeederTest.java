package com.sevasahayog.donationmatching.config;

import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminSeeder seeder(String email, String password) {
        return new AdminSeeder(userRepository, passwordEncoder, email, password);
    }

    @Test
    void seedsAdminWhenBothCredentialsProvided() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");

        seeder("  Admin@Example.com ", "secret123").run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getName()).isEqualTo("Administrator");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void skipsWhenNoCredentialsProvided() {
        seeder("", "").run();
        seeder(null, null).run();
        verifyNoInteractions(userRepository);
    }

    @Test
    void skipsWhenAdminAlreadyExists() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(
                Optional.of(User.builder().email("admin@example.com").build()));

        seeder("admin@example.com", "secret123").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void failsFastWhenEmailSetButPasswordMissing() {
        assertThatThrownBy(() -> seeder("admin@example.com", "").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void failsFastWhenPasswordSetButEmailMissing() {
        assertThatThrownBy(() -> seeder("", "secret123").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void failsFastWhenPasswordTooShort() {
        assertThatThrownBy(() -> seeder("admin@example.com", "short").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8 characters");
    }
}
