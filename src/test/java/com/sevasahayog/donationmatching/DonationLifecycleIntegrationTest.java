package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.exception.ErrorResponse;
import com.sevasahayog.donationmatching.exception.GlobalExceptionHandler;
import com.sevasahayog.donationmatching.repository.AuditRecordRepository;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DonationLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User newUser(Role role) {
        return userRepository.save(User.builder()
                .name("Donation Test User")
                .email("don-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(role)
                .active(true)
                .build());
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }

    private String donationJson(String title) {
        return "{\"title\":\"" + title + "\",\"description\":\"A test donation\",\"category\":\"FOOD\","
                + "\"quantity\":5,\"quantityUnit\":\"KG\",\"condition\":\"NEW\",\"city\":\"Pune\","
                + "\"locality\":\"Shivajinagar\",\"pincode\":\"411005\"}";
    }

    private Donation newDonation(User donor) {
        return donationRepository.save(Donation.builder()
                .donor(donor)
                .title("Seeded Donation")
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

    @Test
    void donorCanCreateDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson("Winter Clothes Drive")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Winter Clothes Drive"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.donor.id").value(donor.getId()))
                .andExpect(jsonPath("$.photos").isArray());
    }

    @Test
    void createIgnoresDonorIdInBody() throws Exception {
        User donor = newUser(Role.DONOR);
        User otherDonor = newUser(Role.DONOR);
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Owned Correctly\",\"description\":\"x\",\"category\":\"FOOD\","
                                + "\"quantity\":1,\"quantityUnit\":\"PIECES\",\"condition\":\"NEW\",\"city\":\"Pune\","
                                + "\"donorId\":" + otherDonor.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donor.id").value(donor.getId()));
    }

    @Test
    void createIgnoresStatusInBody() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Status Ignored\",\"description\":\"x\",\"category\":\"FOOD\","
                                + "\"quantity\":1,\"quantityUnit\":\"PIECES\",\"condition\":\"NEW\",\"city\":\"Pune\","
                                + "\"status\":\"APPROVED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void unauthenticatedCreateReturns401() throws Exception {
        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson("Unauthenticated")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void receiverCannotCreateDonation() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(receiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson("Wrong Role")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminCannotCreateDonation() throws Exception {
        User admin = newUser(Role.ADMIN);
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson("Admin Create")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void invalidDonationReturns400() throws Exception {
        User donor = newUser(Role.DONOR);
        String token = tokenFor(donor);

        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"x\",\"category\":\"FOOD\","
                                + "\"quantity\":1,\"quantityUnit\":\"PIECES\",\"condition\":\"NEW\",\"city\":\"Pune\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Zero Qty\",\"description\":\"x\",\"category\":\"FOOD\","
                                + "\"quantity\":0,\"quantityUnit\":\"PIECES\",\"condition\":\"NEW\",\"city\":\"Pune\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No City\",\"description\":\"x\",\"category\":\"FOOD\","
                                + "\"quantity\":1,\"quantityUnit\":\"PIECES\",\"condition\":\"NEW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void donorCanViewOwnDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(donation.getId()))
                .andExpect(jsonPath("$.title").value("Seeded Donation"))
                .andExpect(jsonPath("$.donor.id").value(donor.getId()));
    }

    @Test
    void donorCannotViewAnotherDonorsDonation() throws Exception {
        User donorA = newUser(Role.DONOR);
        User donorB = newUser(Role.DONOR);
        Donation donation = newDonation(donorA);
        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void donorCannotViewNonexistentDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(get("/api/donations/{id}", 999999L)
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminCanViewAnyDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);
        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(donation.getId()));
    }

    @Test
    void adminCannotViewNonexistentDonation() throws Exception {
        User admin = newUser(Role.ADMIN);
        mockMvc.perform(get("/api/donations/{id}", 999999L)
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void receiverCannotViewDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        Donation donation = newDonation(donor);
        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void donorCanUpdateEditableFields() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        String token = tokenFor(donor);

        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"quantity\":7,\"city\":\"Mumbai\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.quantity").value(7))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void donorCannotModifyServerControlledFields() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        String token = tokenFor(donor);

        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"id\":999999,\"donorId\":999999}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.donor.id").value(donor.getId()));
    }

    @Test
    void emptyUpdateIsNoOp() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateWithBlankTitleReturns400() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void donorCannotUpdateAfterApproval() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Too Late\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void updateRejectedForNonOwner() throws Exception {
        User donorA = newUser(Role.DONOR);
        User donorB = newUser(Role.DONOR);
        Donation donation = newDonation(donorA);
        mockMvc.perform(patch("/api/donations/{id}", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donorB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijack\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminCanApproveDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void adminCanRejectDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void nonAdminCannotReviewDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void adminCannotReviewAnotherDonorsRejectedViaOwnershipRules() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void approvedDonationCannotBeArbitrarilyReverted() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);
        String token = tokenFor(admin);

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectedDonationCannotBeReapproved() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);
        String token = tokenFor(admin);

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusUpdateWithoutStatusFieldReturns400() throws Exception {
        User admin = newUser(Role.ADMIN);
        User donor = newUser(Role.DONOR);
        Donation donation = newDonation(donor);
        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myReturnsOnlyOwnDonations() throws Exception {
        User donorA = newUser(Role.DONOR);
        User donorB = newUser(Role.DONOR);
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(donorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson("Donor A Item")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(donorB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationJson("Donor B Item")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/donations/my")
                        .header("Authorization", "Bearer " + tokenFor(donorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Donor A Item"));
    }

    @Test
    void myEndpointIsPaginated() throws Exception {
        User donor = newUser(Role.DONOR);
        String token = tokenFor(donor);
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/donations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(donationJson("Item " + i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/donations/my").param("page", "0").param("size", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void adminApproveRecordsAuditEntry() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        var audit = auditRecordRepository.findByEntityTypeAndEntityId("Donation", String.valueOf(donation.getId()));
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).getAction()).isEqualTo("DONATION_APPROVED");
        assertThat(audit.get(0).getActor().getId()).isEqualTo(admin.getId());
    }

    @Test
    void adminRejectRecordsAuditEntry() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        Donation donation = newDonation(donor);

        mockMvc.perform(patch("/api/donations/{id}/status", donation.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk());

        var audit = auditRecordRepository.findByEntityTypeAndEntityId("Donation", String.valueOf(donation.getId()));
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).getAction()).isEqualTo("DONATION_REJECTED");
    }

    @Test
    void optimisticLockFailureMapsTo409() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/donations/5");
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Donation.class, 1L), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void concurrentStaleUpdateThrowsOptimisticLockingFailure() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long id = requiresNew.execute(status -> {
            User donor = newUser(Role.DONOR);
            return newDonation(donor).getId();
        });

        Donation managed = donationRepository.findById(id).orElseThrow();

        requiresNew.executeWithoutResult(s -> {
            Donation other = donationRepository.findById(id).orElseThrow();
            other.setTitle("Concurrent change");
            donationRepository.saveAndFlush(other);
        });

        try {
            managed.setTitle("Stale writer change");
            assertThatThrownBy(() -> donationRepository.flush())
                    .isInstanceOfAny(ObjectOptimisticLockingFailureException.class, OptimisticLockException.class);
        } finally {
            requiresNew.executeWithoutResult(s -> donationRepository.deleteById(id));
        }
    }
}
