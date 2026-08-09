package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtProperties;
import com.sevasahayog.donationmatching.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    private static final String PROTECTED_PATH = "/api/donations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtService jwtService;

    private String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@example.com";
    }

    private User newUser(String email, String rawPassword, boolean active) {
        return userRepository.save(User.builder()
                .name("Auth User")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.DONOR)
                .active(active)
                .build());
    }

    @Test
    @Transactional
    void successfulRegistrationReturnsTokenAndDonorRole() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice Donor\",\"email\":\"" + email
                                + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("DONOR"));
    }

    @Test
    @Transactional
    void registrationNormalizesEmailToLowerCase() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Normalized User\",\"email\":\"Alice.USER@Example.COM\","
                                + "\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice.user@example.com"));

        assertThat(userRepository.findByEmail("alice.user@example.com")).isPresent();
    }

    @Test
    @Transactional
    void registrationWithWhitespacePaddedEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Padded Email\",\"email\":\"  alice@example.com  \","
                                + "\"password\":\"Password123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void duplicateEmailReturns409EvenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"First\",\"email\":\"" + email
                                + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second\",\"email\":\"" + email.toUpperCase()
                                + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @Transactional
    void registrationStoresPasswordHashed() throws Exception {
        String email = uniqueEmail();
        String rawPassword = "Password123!";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hash Check\",\"email\":\"" + email
                                + "\",\"password\":\"" + rawPassword + "\"}"))
                .andExpect(status().isCreated());

        Optional<User> saved = userRepository.findByEmail(email);
        assertThat(saved).isPresent();
        assertThat(saved.get().getPassword()).isNotEqualTo(rawPassword);
        assertThat(saved.get().getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, saved.get().getPassword())).isTrue();
    }

    @Test
    @Transactional
    void registrationCannotCreateAdminOrReceiverRoles() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Role Ignored\",\"email\":\"" + email
                                + "\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DONOR"));

        String email2 = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Role Ignored 2\",\"email\":\"" + email2
                                + "\",\"password\":\"Password123!\",\"role\":\"RECEIVER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DONOR"));
    }

    @Test
    @Transactional
    void passwordNeverAppearsInApiResponses() throws Exception {
        String email = uniqueEmail();
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Leak\",\"email\":\"" + email
                                + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(response.toLowerCase()).doesNotContain("password");
    }

    @Test
    @Transactional
    void registrationWithInvalidPayloadReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void successfulLoginReturnsTokenAndRole() throws Exception {
        String email = uniqueEmail();
        newUser(email, "Password123!", true);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("DONOR"));
    }

    @Test
    @Transactional
    void loginNormalizesEmailToLowerCase() throws Exception {
        String email = uniqueEmail();
        newUser(email, "Password123!", true);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @Transactional
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = uniqueEmail();
        newUser(email, "Password123!", true);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"WrongPassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @Transactional
    void loginWithUnknownEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @Transactional
    void loginWithInactiveUserReturns401() throws Exception {
        String email = uniqueEmail();
        newUser(email, "Password123!", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @Transactional
    void loginWithBlankPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void malformedJwtReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void expiredJwtReturns401() throws Exception {
        Instant expired = Instant.now().minus(2, ChronoUnit.HOURS);
        String token = Jwts.builder()
                .subject("expired@example.com")
                .issuedAt(Date.from(expired))
                .expiration(Date.from(expired))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void invalidSignatureJwtReturns401() throws Exception {
        byte[] differentSecret =
                "a-completely-different-secret-that-is-not-the-test-secret-0123456".getBytes(StandardCharsets.UTF_8);
        String token = Jwts.builder()
                .subject("forged@example.com")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(differentSecret))
                .compact();
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @Transactional
    void validJwtAuthenticatesAndReachesProtectedRequest() throws Exception {
        String email = uniqueEmail();
        newUser(email, "Password123!", true);
        String token = jwtService.generateToken(email);
        // A truly unmatched path: a valid token passes the security filter (no
        // 401) and is answered by the not-found handler. /api/donations is a
        // real endpoint since Phase 8.
        mockMvc.perform(get("/api/no-such-endpoint").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void tokenForDeletedOrUnknownUserReturns401() throws Exception {
        String token = jwtService.generateToken("ghost@example.com");
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
