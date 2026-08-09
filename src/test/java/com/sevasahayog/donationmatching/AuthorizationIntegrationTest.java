package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.Urgency;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationIntegrationTest {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String uniqueEmail() {
        return "authz-" + UUID.randomUUID() + "@example.com";
    }

    private User newUser(Role role) {
        return userRepository.save(User.builder()
                .name("Authz User")
                .email(uniqueEmail())
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .role(role)
                .active(true)
                .build());
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }

    private Donation newDonation(User donor) {
        return donationRepository.save(Donation.builder()
                .donor(donor)
                .title("Test Donation")
                .description("A test donation")
                .category(Category.FOOD)
                .quantity(new BigDecimal("5"))
                .quantityUnit(QuantityUnit.KG)
                .condition(Condition.NEW)
                .city("Pune")
                .locality("Shivajinagar")
                .pincode("411005")
                .build());
    }

    private Requirement newRequirement(User receiver) {
        return requirementRepository.save(Requirement.builder()
                .receiver(receiver)
                .title("Test Requirement")
                .description("A test requirement")
                .category(Category.FOOD)
                .quantityRequired(new BigDecimal("2"))
                .quantityUnit(QuantityUnit.KG)
                .city("Pune")
                .locality("Shivajinagar")
                .pincode("411005")
                .urgency(Urgency.MEDIUM)
                .build());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void donorTokenAuthenticates() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(donor.getEmail()))
                .andExpect(jsonPath("$.role").value("DONOR"));
    }

    @Test
    void receiverTokenAuthenticates() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECEIVER"));
    }

    @Test
    void adminTokenAuthenticates() throws Exception {
        User admin = newUser(Role.ADMIN);
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void meEndpointDoesNotExposePassword() throws Exception {
        User donor = newUser(Role.DONOR);
        String body = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body.toLowerCase()).doesNotContain("password");
    }

    @Test
    void donorCanAccessDonorOnlyEndpoint() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Test Donation"));
    }

    @Test
    void donorCannotAccessReceiverOnlyEndpoint() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        Requirement requirement = newRequirement(receiver);
        mockMvc.perform(get("/api/requirements/{id}", requirement.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void donorCannotAccessAdminEndpoint() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(get("/api/admin/queue").header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCanAccessReceiverOnlyEndpoint() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        Requirement requirement = newRequirement(receiver);
        mockMvc.perform(get("/api/requirements/{id}", requirement.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Test Requirement"));
    }

    @Test
    void receiverCannotAccessDonorOnlyEndpoint() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCannotAccessAdminEndpoint() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(get("/api/admin/queue").header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        User admin = newUser(Role.ADMIN);
        mockMvc.perform(get("/api/admin/queue").header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanReadButCannotModifyDonorOrReceiverResources() throws Exception {
        User admin = newUser(Role.ADMIN);
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        Donation donation = newDonation(donor);
        Requirement requirement = newRequirement(receiver);
        String token = tokenFor(admin);

        mockMvc.perform(get("/api/donations/{id}", donation.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/requirements/{id}", requirement.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/requirements/{id}", requirement.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationCannotCreateAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Escalation\",\"email\":\"" + uniqueEmail()
                                + "\",\"password\":\"" + RAW_PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DONOR"));
    }

    @Test
    void authenticatedUserCannotEscalateOwnRole() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        String token = tokenFor(donor);

        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"donorId\":999999}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DONOR"));

        User reloaded = userRepository.findById(donor.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.DONOR);
    }
}
