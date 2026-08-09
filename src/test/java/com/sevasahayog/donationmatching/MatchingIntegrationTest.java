package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.Urgency;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.MatchRepository;
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.TransactionRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchingIntegrationTest {

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
                .name("Matching Test User")
                .email("matching-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(role)
                .active(true)
                .build());
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }

    private Donation donation(User donor, String title, Category category, String quantity,
                              QuantityUnit unit, String city, DonationStatus status) {
        return donationRepository.save(Donation.builder()
                .donor(donor)
                .title(title)
                .description("Description for " + title)
                .category(category)
                .quantity(new BigDecimal(quantity))
                .quantityUnit(unit)
                .condition(Condition.NEW)
                .city(city)
                .locality("Shivajinagar")
                .pincode("411005")
                .status(status)
                .build());
    }

    private Requirement requirement(User receiver, String title, Category category, String quantity,
                                    QuantityUnit unit, String city, Urgency urgency, RequirementStatus status) {
        return requirementRepository.save(Requirement.builder()
                .receiver(receiver)
                .title(title)
                .description("Description for " + title)
                .category(category)
                .quantityRequired(new BigDecimal(quantity))
                .quantityUnit(unit)
                .city(city)
                .locality("Shivajinagar")
                .pincode("411005")
                .urgency(urgency)
                .status(status)
                .build());
    }

    private ResultActions suggest(String token, long requirementId) throws Exception {
        return mockMvc.perform(post("/api/admin/matches/suggest")
                .param("requirementId", String.valueOf(requirementId))
                .header("Authorization", "Bearer " + token));
    }

    // ------------------------------------------------------------------
    // Hard gates
    // ------------------------------------------------------------------

    @Test
    void unapprovedDonationIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Pending Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.SUBMITTED);
        Requirement requirement = requirement(receiver, "Food Needed", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(matchRepository.findByRequirementId(requirement.getId())).isEmpty();
    }

    @Test
    void unapprovedRequirementIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Approved Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Pending Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.SUBMITTED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(matchRepository.findByRequirementId(requirement.getId())).isEmpty();
    }

    @Test
    void categoryMismatchIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Food Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Medical Need", Category.MEDICAL, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void quantityUnitMismatchIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Kilogram Donation", Category.FOOD, "100", QuantityUnit.KG, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Piece Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void insufficientDonationQuantityIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Small Donation", Category.FOOD, "5", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Big Need", Category.FOOD, "10", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void differentCityIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Pune Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Mumbai Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Mumbai", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cityMatchIsCaseInsensitive() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Pune Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Pune Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ------------------------------------------------------------------
    // Scoring
    // ------------------------------------------------------------------

    @Test
    void exactQuantityScoresFullQuantityPoints() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Exact Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Exact Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SUGGESTED"))
                .andExpect(jsonPath("$[0].score").value(92.0))
                .andExpect(jsonPath("$[0].breakdown.categoryScore").value(30.0))
                .andExpect(jsonPath("$[0].breakdown.quantityScore").value(30.0))
                .andExpect(jsonPath("$[0].breakdown.locationScore").value(20.0))
                .andExpect(jsonPath("$[0].breakdown.urgencyScore").value(12.0))
                .andExpect(jsonPath("$[0].breakdown.totalScore").value(92.0));
    }

    @Test
    void largerDonationScoresProportionally() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Double Donation", Category.FOOD, "200", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Need Half", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(77.0))
                .andExpect(jsonPath("$[0].breakdown.quantityScore").value(15.0));
    }

    @Test
    void urgencyScoresVaryByLevel() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Urgency Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement low = requirement(receiver, "Low Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.LOW, RequirementStatus.APPROVED);
        Requirement medium = requirement(receiver, "Medium Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);
        Requirement high = requirement(receiver, "High Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.HIGH, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), low.getId())
                .andExpect(jsonPath("$[0].score").value(85.0))
                .andExpect(jsonPath("$[0].breakdown.urgencyScore").value(5.0));
        suggest(tokenFor(admin), medium.getId())
                .andExpect(jsonPath("$[0].score").value(92.0))
                .andExpect(jsonPath("$[0].breakdown.urgencyScore").value(12.0));
        suggest(tokenFor(admin), high.getId())
                .andExpect(jsonPath("$[0].score").value(100.0))
                .andExpect(jsonPath("$[0].breakdown.urgencyScore").value(20.0));
    }

    @Test
    void scoreEqualToThresholdIsSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Threshold Donation", Category.FOOD, "200", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Threshold Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.LOW, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].score").value(70.0));
    }

    @Test
    void scoreBelowThresholdIsNotSuggested() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Low Score Donation", Category.FOOD, "300", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Large Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.LOW, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(matchRepository.findByRequirementId(requirement.getId())).isEmpty();
    }

    // ------------------------------------------------------------------
    // Suggestion behaviour
    // ------------------------------------------------------------------

    @Test
    void suggestionIsCappedAtFivePerRequirement() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        for (int i = 1; i <= 6; i++) {
            donation(donor, "Candidate " + i, Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        }
        Requirement requirement = requirement(receiver, "Capped Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        assertThat(matchRepository.findByRequirementId(requirement.getId())).hasSize(5);
    }

    @Test
    void suggestionsAreReturnedInDescendingScoreOrder() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation better = donation(donor, "Better Fit", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Donation weaker = donation(donor, "Weaker Fit", Category.FOOD, "200", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Ordered Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.LOW, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].donation.id").value(better.getId()))
                .andExpect(jsonPath("$[0].score").value(85.0))
                .andExpect(jsonPath("$[1].donation.id").value(weaker.getId()))
                .andExpect(jsonPath("$[1].score").value(70.0));
    }

    @Test
    void equalScoresUseDeterministicOrderByDonationId() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation first = donation(donor, "First Donation", Category.FOOD, "200", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Donation second = donation(donor, "Second Donation", Category.FOOD, "200", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Tie Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.LOW, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].donation.id").value(first.getId()))
                .andExpect(jsonPath("$[1].donation.id").value(second.getId()));
    }

    @Test
    void runningSuggestAgainDoesNotDuplicateMatches() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "One-Off Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Once Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(matchRepository.findByRequirementId(requirement.getId())).hasSize(1);
    }

    @Test
    void sameDonationCanBeSuggestedForMultipleRequirements() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, "Shared Donation", Category.FOOD, "200", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement first = requirement(receiver, "First Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);
        Requirement second = requirement(receiver, "Second Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), first.getId())
                .andExpect(jsonPath("$.length()").value(1));
        suggest(tokenFor(admin), second.getId())
                .andExpect(jsonPath("$.length()").value(1));

        assertThat(matchRepository.findByRequirementId(first.getId()).get(0).getDonation().getId())
                .isEqualTo(donation.getId());
        assertThat(matchRepository.findByRequirementId(second.getId()).get(0).getDonation().getId())
                .isEqualTo(donation.getId());
        assertThat(matchRepository.findByRequirementId(first.getId())).hasSize(1);
        assertThat(matchRepository.findByRequirementId(second.getId())).hasSize(1);
    }

    @Test
    void matchingDoesNotChangeDonationOrRequirementStatus() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        Donation donation = donation(donor, "Stable Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Stable Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(jsonPath("$.length()").value(1));

        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.APPROVED);
        assertThat(requirementRepository.findById(requirement.getId()).orElseThrow().getStatus())
                .isEqualTo(RequirementStatus.APPROVED);
    }

    @Test
    void matchingDoesNotCreateTransaction() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "No-Transaction Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "No-Transaction Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        suggest(tokenFor(admin), requirement.getId())
                .andExpect(jsonPath("$.length()").value(1));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void suggestForAllReturnsSummary() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Bulk Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        requirement(receiver, "Bulk Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);

        mockMvc.perform(post("/api/admin/matches/suggest")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementsEvaluated").value(1))
                .andExpect(jsonPath("$.suggestionsCreated").value(1));
    }

    @Test
    void nonexistentRequirementReturns404() throws Exception {
        User admin = newUser(Role.ADMIN);
        suggest(tokenFor(admin), 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ------------------------------------------------------------------
    // Admin match queue
    // ------------------------------------------------------------------

    @Test
    void adminCanListSuggestedMatches() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Rice Bags", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "School Meals", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());

        mockMvc.perform(get("/api/admin/matches")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SUGGESTED"))
                .andExpect(jsonPath("$.content[0].score").value(92.0))
                .andExpect(jsonPath("$.content[0].donation.title").value("Rice Bags"))
                .andExpect(jsonPath("$.content[0].requirement.title").value("School Meals"));
    }

    @Test
    void adminCanFilterMatchQueueByStatus() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Filtered Donation", Category.FOOD, "100", QuantityUnit.PIECES, "Pune", DonationStatus.APPROVED);
        Requirement requirement = requirement(receiver, "Filtered Need", Category.FOOD, "100", QuantityUnit.PIECES,
                "Pune", Urgency.MEDIUM, RequirementStatus.APPROVED);
        suggest(tokenFor(admin), requirement.getId());

        mockMvc.perform(get("/api/admin/matches")
                        .param("status", "SUGGESTED")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/admin/matches")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ------------------------------------------------------------------
    // Authorization
    // ------------------------------------------------------------------

    @Test
    void donorCannotTriggerMatching() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(post("/api/admin/matches/suggest")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCannotTriggerMatching() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(post("/api/admin/matches/suggest")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void unauthenticatedCannotTriggerMatching() throws Exception {
        mockMvc.perform(post("/api/admin/matches/suggest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void donorCannotListMatches() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(get("/api/admin/matches")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void receiverCannotListMatches() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(get("/api/admin/matches")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
