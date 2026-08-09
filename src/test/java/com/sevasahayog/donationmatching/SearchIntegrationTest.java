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
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchIntegrationTest {

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

    private User newUser(Role role) {
        return userRepository.save(User.builder()
                .name("Search Test User")
                .email("search-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(role)
                .active(true)
                .build());
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getEmail());
    }

    private Donation donation(User donor, String title, Category category, String city, DonationStatus status) {
        return donationRepository.save(Donation.builder()
                .donor(donor)
                .title(title)
                .description("Description for " + title)
                .category(category)
                .quantity(new BigDecimal("5"))
                .quantityUnit(QuantityUnit.KG)
                .condition(Condition.NEW)
                .city(city)
                .locality("Shivajinagar")
                .pincode("411005")
                .status(status)
                .build());
    }

    private Requirement requirement(User receiver, String title, Category category, String city,
                                    RequirementStatus status) {
        return requirementRepository.save(Requirement.builder()
                .receiver(receiver)
                .title(title)
                .description("Description for " + title)
                .category(category)
                .quantityRequired(new BigDecimal("2"))
                .quantityUnit(QuantityUnit.KG)
                .city(city)
                .locality("Shivajinagar")
                .pincode("411005")
                .urgency(Urgency.MEDIUM)
                .status(status)
                .build());
    }

    // ------------------------------------------------------------------
    // Donation discovery
    // ------------------------------------------------------------------

    @Test
    void receiverCanSearchApprovedDonations() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Rice Bags", Category.FOOD, "Pune", DonationStatus.APPROVED);

        mockMvc.perform(get("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Rice Bags"))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].donor.id").value(donor.getId()));
    }

    @Test
    void receiverDoesNotSeeRejectedDonations() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Approved Item", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Rejected Item", Category.FOOD, "Pune", DonationStatus.REJECTED);

        mockMvc.perform(get("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Approved Item"))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    @Test
    void receiverDoesNotSeeSubmittedDonations() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Approved Item", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Pending Item", Category.FOOD, "Pune", DonationStatus.SUBMITTED);
        donation(donor, "Rejected Item", Category.FOOD, "Pune", DonationStatus.REJECTED);

        mockMvc.perform(get("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Approved Item"));
    }

    @Test
    void donationCategoryFilterWorks() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Food Item", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Clothing Item", Category.CLOTHING, "Pune", DonationStatus.APPROVED);

        mockMvc.perform(get("/api/donations")
                        .param("category", "FOOD")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Food Item"))
                .andExpect(jsonPath("$.content[0].category").value("FOOD"));
    }

    @Test
    void donationCityFilterWorksAndIsCaseInsensitive() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Pune Item", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Mumbai Item", Category.FOOD, "Mumbai", DonationStatus.APPROVED);

        mockMvc.perform(get("/api/donations")
                        .param("city", "pune")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Pune Item"))
                .andExpect(jsonPath("$.content[0].city").value("Pune"));
    }

    @Test
    void donationPaginationWorks() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        String token = tokenFor(receiver);
        for (int i = 1; i <= 5; i++) {
            donation(donor, "Paged Item " + i, Category.FOOD, "Pune", DonationStatus.APPROVED);
        }

        mockMvc.perform(get("/api/donations").param("page", "0").param("size", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0));

        mockMvc.perform(get("/api/donations").param("page", "1").param("size", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(get("/api/donations").param("page", "2").param("size", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void adminCanSearchDonations() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Approved Item", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Pending Item", Category.FOOD, "Pune", DonationStatus.SUBMITTED);
        donation(donor, "Rejected Item", Category.FOOD, "Pune", DonationStatus.REJECTED);

        mockMvc.perform(get("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void adminCanFilterDonationsByStatus() throws Exception {
        User donor = newUser(Role.DONOR);
        User admin = newUser(Role.ADMIN);
        donation(donor, "Approved Item", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Pending Item", Category.FOOD, "Pune", DonationStatus.SUBMITTED);
        donation(donor, "Rejected Item", Category.FOOD, "Pune", DonationStatus.REJECTED);

        mockMvc.perform(get("/api/donations")
                        .param("status", "SUBMITTED")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Pending Item"));

        mockMvc.perform(get("/api/donations")
                        .param("status", "REJECTED")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Rejected Item"));
    }

    @Test
    void donorCannotUseDonationSearch() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(get("/api/donations")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void unauthenticatedDonationSearchReturns401() throws Exception {
        mockMvc.perform(get("/api/donations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void receiverCannotFilterDonationsByStatus() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Approved Item", Category.FOOD, "Pune", DonationStatus.APPROVED);

        mockMvc.perform(get("/api/donations")
                        .param("status", "REJECTED")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void invalidDonationCategoryReturns400() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(get("/api/donations")
                        .param("category", "NOT_A_CATEGORY")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void invalidDonationStatusReturns400() throws Exception {
        User admin = newUser(Role.ADMIN);
        mockMvc.perform(get("/api/donations")
                        .param("status", "NOT_A_STATUS")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void donationTextSearchMatchesTitleAndDescription() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Rice Bags", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Blankets", Category.CLOTHING, "Pune", DonationStatus.APPROVED);
        donationRepository.save(Donation.builder()
                .donor(donor)
                .title("Winter Kit")
                .description("urgent cold weather supplies")
                .category(Category.MEDICAL)
                .quantity(new BigDecimal("3"))
                .quantityUnit(QuantityUnit.PIECES)
                .condition(Condition.NEW)
                .city("Pune")
                .status(DonationStatus.APPROVED)
                .build());

        mockMvc.perform(get("/api/donations")
                        .param("query", "rice")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Rice Bags"));

        mockMvc.perform(get("/api/donations")
                        .param("query", "urgent")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Winter Kit"));

        mockMvc.perform(get("/api/donations")
                        .param("query", "nothing-matches")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidDonationSortPropertyReturns400() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(get("/api/donations")
                        .param("sort", "title")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void negativeDonationPageIsNormalizedToZero() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Rice Bags", Category.FOOD, "Pune", DonationStatus.APPROVED);

        mockMvc.perform(get("/api/donations")
                        .param("page", "-1")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void nonNumericDonationSizeFallsBackToDefaultPageSize() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        for (int i = 1; i <= 25; i++) {
            donation(donor, "Bulk Item " + i, Category.FOOD, "Pune", DonationStatus.APPROVED);
        }

        mockMvc.perform(get("/api/donations")
                        .param("size", "abc")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.content.length()").value(20));
    }

    @Test
    void oversizedDonationSizeIsClampedToMaxPageSize() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        for (int i = 1; i <= 105; i++) {
            donation(donor, "Bulk Item " + i, Category.FOOD, "Pune", DonationStatus.APPROVED);
        }

        mockMvc.perform(get("/api/donations")
                        .param("size", "500")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(100))
                .andExpect(jsonPath("$.totalElements").value(105))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void donationCombinedCategoryCityAndPaginationSatisfyAllFilters() throws Exception {
        User donor = newUser(Role.DONOR);
        User receiver = newUser(Role.RECEIVER);
        donation(donor, "Food Pune 1", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Food Pune 2", Category.FOOD, "Pune", DonationStatus.APPROVED);
        donation(donor, "Food Mumbai", Category.FOOD, "Mumbai", DonationStatus.APPROVED);
        donation(donor, "Clothing Pune", Category.CLOTHING, "Pune", DonationStatus.APPROVED);
        donation(donor, "Food Pune Pending", Category.FOOD, "Pune", DonationStatus.SUBMITTED);
        donation(donor, "Food Pune Rejected", Category.FOOD, "Pune", DonationStatus.REJECTED);

        mockMvc.perform(get("/api/donations")
                        .param("category", "FOOD")
                        .param("city", "Pune")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].category").value("FOOD"))
                .andExpect(jsonPath("$.content[0].city").value("Pune"))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    // ------------------------------------------------------------------
    // Requirement discovery
    // ------------------------------------------------------------------

    @Test
    void donorCanSearchApprovedRequirements() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "School Books", Category.EDUCATION, "Pune", RequirementStatus.APPROVED);

        mockMvc.perform(get("/api/requirements")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("School Books"))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].receiver.id").value(receiver.getId()));
    }

    @Test
    void donorDoesNotSeeRejectedRequirements() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "Approved Request", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Rejected Request", Category.FOOD, "Pune", RequirementStatus.REJECTED);

        mockMvc.perform(get("/api/requirements")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Approved Request"));
    }

    @Test
    void donorDoesNotSeeSubmittedRequirements() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "Approved Request", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Pending Request", Category.FOOD, "Pune", RequirementStatus.SUBMITTED);
        requirement(receiver, "Rejected Request", Category.FOOD, "Pune", RequirementStatus.REJECTED);

        mockMvc.perform(get("/api/requirements")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Approved Request"));
    }

    @Test
    void requirementCategoryFilterWorks() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "Food Request", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Clothing Request", Category.CLOTHING, "Pune", RequirementStatus.APPROVED);

        mockMvc.perform(get("/api/requirements")
                        .param("category", "CLOTHING")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clothing Request"))
                .andExpect(jsonPath("$.content[0].category").value("CLOTHING"));
    }

    @Test
    void requirementCityFilterWorks() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "Pune Request", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Mumbai Request", Category.FOOD, "Mumbai", RequirementStatus.APPROVED);

        mockMvc.perform(get("/api/requirements")
                        .param("city", "Mumbai")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Mumbai Request"))
                .andExpect(jsonPath("$.content[0].city").value("Mumbai"));
    }

    @Test
    void requirementPaginationWorks() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        String token = tokenFor(donor);
        for (int i = 1; i <= 4; i++) {
            requirement(receiver, "Request " + i, Category.FOOD, "Pune", RequirementStatus.APPROVED);
        }

        mockMvc.perform(get("/api/requirements").param("page", "0").param("size", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/requirements").param("page", "1").param("size", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void adminCanSearchRequirements() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User admin = newUser(Role.ADMIN);
        requirement(receiver, "Approved Request", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Pending Request", Category.FOOD, "Pune", RequirementStatus.SUBMITTED);
        requirement(receiver, "Rejected Request", Category.FOOD, "Pune", RequirementStatus.REJECTED);

        mockMvc.perform(get("/api/requirements")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void receiverCannotUseRequirementSearch() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        mockMvc.perform(get("/api/requirements")
                        .header("Authorization", "Bearer " + tokenFor(receiver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void unauthenticatedRequirementSearchReturns401() throws Exception {
        mockMvc.perform(get("/api/requirements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void donorCannotFilterRequirementsByStatus() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "Approved Request", Category.FOOD, "Pune", RequirementStatus.APPROVED);

        mockMvc.perform(get("/api/requirements")
                        .param("status", "REJECTED")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void invalidRequirementCategoryReturns400() throws Exception {
        User donor = newUser(Role.DONOR);
        mockMvc.perform(get("/api/requirements")
                        .param("category", "NOT_A_CATEGORY")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void requirementTextSearchWorks() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "First Aid Kits", Category.MEDICAL, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Notebooks", Category.EDUCATION, "Pune", RequirementStatus.APPROVED);

        mockMvc.perform(get("/api/requirements")
                        .param("query", "first aid")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("First Aid Kits"));

        mockMvc.perform(get("/api/requirements")
                        .param("query", "no-such-term")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void requirementCombinedCategoryCityAndPaginationSatisfyAllFilters() throws Exception {
        User receiver = newUser(Role.RECEIVER);
        User donor = newUser(Role.DONOR);
        requirement(receiver, "Food Pune 1", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Food Pune 2", Category.FOOD, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Food Mumbai", Category.FOOD, "Mumbai", RequirementStatus.APPROVED);
        requirement(receiver, "Medical Pune", Category.MEDICAL, "Pune", RequirementStatus.APPROVED);
        requirement(receiver, "Food Pune Pending", Category.FOOD, "Pune", RequirementStatus.SUBMITTED);
        requirement(receiver, "Food Pune Rejected", Category.FOOD, "Pune", RequirementStatus.REJECTED);

        mockMvc.perform(get("/api/requirements")
                        .param("category", "FOOD")
                        .param("city", "Pune")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].category").value("FOOD"))
                .andExpect(jsonPath("$.content[0].city").value("Pune"))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }
}
