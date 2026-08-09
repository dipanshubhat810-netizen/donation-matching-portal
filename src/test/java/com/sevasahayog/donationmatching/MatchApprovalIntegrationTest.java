package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.Transaction;
import com.sevasahayog.donationmatching.entity.TransactionStatus;
import com.sevasahayog.donationmatching.entity.Urgency;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.MatchRepository;
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.TransactionRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchApprovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM audit_records");
        jdbcTemplate.update("DELETE FROM donation_photos");
        jdbcTemplate.update("DELETE FROM donations");
        jdbcTemplate.update("DELETE FROM requirements");
        jdbcTemplate.update("DELETE FROM users");
    }

    private User newUser(Role role) {
        return userRepository.save(User.builder()
                .name("Approval Test User")
                .email("approval-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(role)
                .active(true)
                .build());
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }

    private Donation donation(User donor, DonationStatus status) {
        return donationRepository.save(Donation.builder()
                .donor(donor)
                .title("Rice Bags")
                .description("Donated rice for distribution")
                .category(Category.FOOD)
                .quantity(new BigDecimal("100"))
                .quantityUnit(QuantityUnit.PIECES)
                .condition(Condition.NEW)
                .city("Pune")
                .locality("Shivajinagar")
                .pincode("411005")
                .status(status)
                .build());
    }

    private Requirement requirement(User receiver, RequirementStatus status) {
        return requirementRepository.save(Requirement.builder()
                .receiver(receiver)
                .title("School Meals")
                .description("Food needed for school lunch programme")
                .category(Category.FOOD)
                .quantityRequired(new BigDecimal("100"))
                .quantityUnit(QuantityUnit.PIECES)
                .city("Pune")
                .locality("Shivajinagar")
                .pincode("411005")
                .urgency(Urgency.MEDIUM)
                .status(status)
                .build());
    }

    private ResultActions suggest(String token, long requirementId) throws Exception {
        return mockMvc.perform(post("/api/admin/matches/suggest")
                .param("requirementId", String.valueOf(requirementId))
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions approve(String token, long matchId) throws Exception {
        return mockMvc.perform(post("/api/admin/matches/" + matchId + "/approve")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions reject(String token, long matchId) throws Exception {
        return mockMvc.perform(post("/api/admin/matches/" + matchId + "/reject")
                .header("Authorization", "Bearer " + token));
    }

    private Match suggestedMatch(User admin, User donor, User receiver) throws Exception {
        donation(donor, DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());
        return matchRepository.findByRequirementId(requirement.getId()).get(0);
    }

    // ------------------------------------------------------------------
    // Approval
    // ------------------------------------------------------------------

    @Test
    void adminCanApproveSuggestedMatch() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);

        approve(tokenFor(admin), match.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy.name").value(admin.getName()))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());

        Match updated = matchRepository.findById(match.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MatchStatus.APPROVED);
        assertThat(updated.getReviewedBy().getId()).isEqualTo(admin.getId());
        assertThat(updated.getReviewedAt()).isNotNull();
    }

    @Test
    void approvalMarksDonationAsMatched() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());
        Match match = matchRepository.findByRequirementId(requirement.getId()).get(0);

        approve(tokenFor(admin), match.getId())
                .andExpect(status().isOk());

        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.MATCHED);
    }

    @Test
    void approvalCreatesPendingTransactionWithParties() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);

        approve(tokenFor(admin), match.getId())
                .andExpect(status().isOk());

        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getDonor().getId()).isEqualTo(donor.getId());
        assertThat(transaction.getReceiver().getId()).isEqualTo(receiver.getId());
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void approvalIsAudited() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);

        approve(tokenFor(admin), match.getId())
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_records WHERE entity_type = 'Match' AND action = 'MATCH_APPROVED'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void approvingApprovedMatchIsRejected() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());

        approve(tokenFor(admin), match.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void approvingConsumedDonationIsRejected() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.APPROVED);
        Requirement first = requirement(receiver, RequirementStatus.APPROVED);
        Requirement second = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), first.getId());
        suggest(tokenFor(admin), second.getId());
        Match approved = matchRepository.findByRequirementId(first.getId()).get(0);
        Match other = matchRepository.findByRequirementId(second.getId()).get(0);

        approve(tokenFor(admin), approved.getId()).andExpect(status().isOk());

        approve(tokenFor(admin), other.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.MATCHED);
    }

    @Test
    void approvingNonexistentMatchReturns404() throws Exception {
        User admin = newUser(Role.ADMIN);
        approve(tokenFor(admin), 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ------------------------------------------------------------------
    // Rejection
    // ------------------------------------------------------------------

    @Test
    void adminCanRejectSuggestedMatch() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);

        reject(tokenFor(admin), match.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewedBy.name").value(admin.getName()));

        assertThat(matchRepository.findById(match.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.REJECTED);
    }

    @Test
    void rejectionDoesNotCreateTransactionOrChangeDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());
        Match match = matchRepository.findByRequirementId(requirement.getId()).get(0);

        reject(tokenFor(admin), match.getId()).andExpect(status().isOk());

        assertThat(transactionRepository.count()).isZero();
        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.APPROVED);
    }

    @Test
    void cannotRejectAlreadyApprovedMatch() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());

        reject(tokenFor(admin), match.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void cannotApproveRejectedMatch() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        reject(tokenFor(admin), match.getId()).andExpect(status().isOk());

        approve(tokenFor(admin), match.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ------------------------------------------------------------------
    // Transactions
    // ------------------------------------------------------------------

    @Test
    void adminCanListTransactions() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/transactions")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].matchId").value(match.getId()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].donor.name").value(donor.getName()))
                .andExpect(jsonPath("$.content[0].receiver.name").value(receiver.getName()));
    }

    @Test
    void adminCanFilterTransactionsByStatus() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/transactions")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/admin/transactions")
                        .param("status", "COMPLETED")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void startMovesTransactionToInProgressAndMatchToInFulfilment() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertThat(matchRepository.findById(match.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.IN_FULFILMENT);
        assertThat(donationRepository.findById(match.getDonation().getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.IN_FULFILMENT);
    }

    @Test
    void cannotStartNonPendingTransaction() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();
        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void completeFinishesMatchDonationAndRequirement() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());
        Match match = matchRepository.findByRequirementId(requirement.getId()).get(0);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();
        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        assertThat(matchRepository.findById(match.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.COMPLETED);
        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.COMPLETED);
        assertThat(requirementRepository.findById(requirement.getId()).orElseThrow().getStatus())
                .isEqualTo(RequirementStatus.FULFILLED);
    }

    @Test
    void cannotCompleteBeforeStart() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void cancellingPendingTransactionReleasesDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());
        Match match = matchRepository.findByRequirementId(requirement.getId()).get(0);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(matchRepository.findById(match.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.CANCELLED);
        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.APPROVED);
        assertThat(requirementRepository.findById(requirement.getId()).orElseThrow().getStatus())
                .isEqualTo(RequirementStatus.APPROVED);
    }

    @Test
    void cancellingInProgressTransactionReleasesDonation() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());
        Match match = matchRepository.findByRequirementId(requirement.getId()).get(0);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();
        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(matchRepository.findById(match.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.CANCELLED);
        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.APPROVED);
    }

    @Test
    void cannotCancelCompletedTransaction() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();
        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/complete")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void transactionActionOnNonexistentTransactionReturns404() throws Exception {
        User admin = newUser(Role.ADMIN);
        mockMvc.perform(post("/api/admin/transactions/999999/start")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ------------------------------------------------------------------
    // Authorization
    // ------------------------------------------------------------------

    @Test
    void donorCannotApproveMatches() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(post("/api/admin/matches/1/approve")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCannotApproveMatches() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(post("/api/admin/matches/1/approve")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void unauthenticatedCannotApproveMatches() throws Exception {
        mockMvc.perform(post("/api/admin/matches/1/approve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void donorCannotManageTransactions() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(post("/api/admin/transactions/1/start")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
        mockMvc.perform(get("/api/admin/transactions")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Phase 12: audit trail, concurrency, defensive rules
    // ------------------------------------------------------------------

    @Test
    void transactionStateChangesAreAudited() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/start")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/complete")
                .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        Integer approvals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_records WHERE action = 'MATCH_APPROVED'", Integer.class);
        Integer started = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_records WHERE action = 'TRANSACTION_STARTED'", Integer.class);
        Integer completed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_records WHERE action = 'TRANSACTION_COMPLETED'", Integer.class);
        assertThat(approvals).isEqualTo(1);
        assertThat(started).isEqualTo(1);
        assertThat(completed).isEqualTo(1);
    }

    @Test
    void cancelIsAudited() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Match match = suggestedMatch(admin, donor, receiver);
        approve(tokenFor(admin), match.getId()).andExpect(status().isOk());
        Transaction transaction = transactionRepository.findByMatchId(match.getId()).orElseThrow();

        mockMvc.perform(post("/api/admin/transactions/" + transaction.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        Integer cancelled = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_records WHERE action = 'TRANSACTION_CANCELLED'", Integer.class);
        assertThat(cancelled).isEqualTo(1);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void staleMatchApprovalThrowsOptimisticLockFailure() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        Long matchId = requiresNew.execute(status -> {
            User donor = newUser(Role.DONOR);
            User receiver = newUser(Role.RECEIVER);
            Donation donation = donation(donor, DonationStatus.APPROVED);
            Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);
            Match match = matchRepository.save(Match.builder()
                    .donation(donation)
                    .requirement(requirement)
                    .score(new BigDecimal("90.00"))
                    .status(MatchStatus.SUGGESTED)
                    .build());
            return match.getId();
        });

        Match stale = matchRepository.findById(matchId).orElseThrow();

        requiresNew.executeWithoutResult(s -> {
            Match concurrent = matchRepository.findById(matchId).orElseThrow();
            concurrent.setStatus(MatchStatus.REJECTED);
            matchRepository.saveAndFlush(concurrent);
        });

        try {
            stale.setStatus(MatchStatus.APPROVED);
            assertThatThrownBy(() -> matchRepository.flush())
                    .isInstanceOfAny(ObjectOptimisticLockingFailureException.class, OptimisticLockException.class);
        } finally {
            requiresNew.executeWithoutResult(s -> {
                transactionRepository.deleteAll(transactionRepository.findByMatchId(matchId).stream().toList());
                matchRepository.deleteById(matchId);
            });
        }
    }

    @Test
    void completedDonationCannotBeSuggestedAgain() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, DonationStatus.COMPLETED);
        Requirement requirement = requirement(receiver, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(matchRepository.findByRequirementId(requirement.getId())).isEmpty();
        assertThat(donation.getStatus()).isEqualTo(DonationStatus.COMPLETED);
    }
}
