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
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnershipIntegrationTest {

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

    private User donorA;
    private User donorB;
    private User receiverA;
    private User receiverB;
    private Donation donationA;
    private Donation donationB;
    private Requirement requirementA;
    private Requirement requirementB;

    @BeforeEach
    void setUp() {
        donorA = newUser(Role.DONOR);
        donorB = newUser(Role.DONOR);
        receiverA = newUser(Role.RECEIVER);
        receiverB = newUser(Role.RECEIVER);
        donationA = newDonation(donorA);
        donationB = newDonation(donorB);
        requirementA = newRequirement(receiverA);
        requirementB = newRequirement(receiverB);
    }

    private User newUser(Role role) {
        return userRepository.save(User.builder()
                .name("Owner User")
                .email("owner-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .role(role)
                .active(true)
                .build());
    }

    private Donation newDonation(User donor) {
        return donationRepository.save(Donation.builder()
                .donor(donor)
                .title("Donation of " + donor.getEmail())
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
                .title("Requirement of " + receiver.getEmail())
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

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }

    @Test
    void donorCanViewOwnDonation() throws Exception {
        mockMvc.perform(get("/api/donations/{id}", donationA.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Donation of " + donorA.getEmail()));
    }

    @Test
    void donorCannotViewAnotherDonorsDonation() throws Exception {
        mockMvc.perform(get("/api/donations/{id}", donationB.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void donorCanModifyOwnDonation() throws Exception {
        mockMvc.perform(patch("/api/donations/{id}", donationA.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void donorCannotModifyAnotherDonorsDonation() throws Exception {
        mockMvc.perform(patch("/api/donations/{id}", donationB.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCanViewOwnRequirement() throws Exception {
        mockMvc.perform(get("/api/requirements/{id}", requirementA.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiverA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Requirement of " + receiverA.getEmail()));
    }

    @Test
    void receiverCannotViewAnotherReceiversRequirement() throws Exception {
        mockMvc.perform(get("/api/requirements/{id}", requirementB.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiverA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCanModifyOwnRequirement() throws Exception {
        mockMvc.perform(patch("/api/requirements/{id}", requirementA.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiverA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void receiverCannotModifyAnotherReceiversRequirement() throws Exception {
        mockMvc.perform(patch("/api/requirements/{id}", requirementB.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiverA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void idInRequestBodyCannotBypassOwnership() throws Exception {
        mockMvc.perform(patch("/api/donations/{id}", donationA.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + donationB.getId() + ",\"donorId\":" + donorB.getId() + "}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/donations/{id}", donationB.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + donationA.getId() + ",\"donorId\":" + donorA.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void nonexistentResourceDoesNotLeakExistence() throws Exception {
        mockMvc.perform(get("/api/donations/{id}", 999999L)
                        .header("Authorization", "Bearer " + tokenFor(donorA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
