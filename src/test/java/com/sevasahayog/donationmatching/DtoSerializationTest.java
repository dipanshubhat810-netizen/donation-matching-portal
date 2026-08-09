package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.dto.AuditRecordResponse;
import com.sevasahayog.donationmatching.dto.DonationPhotoResponse;
import com.sevasahayog.donationmatching.dto.DonationRequest;
import com.sevasahayog.donationmatching.dto.DonationResponse;
import com.sevasahayog.donationmatching.dto.MatchResponse;
import com.sevasahayog.donationmatching.dto.RequirementResponse;
import com.sevasahayog.donationmatching.dto.TransactionResponse;
import com.sevasahayog.donationmatching.dto.UserResponse;
import com.sevasahayog.donationmatching.dto.UserSummaryResponse;
import com.sevasahayog.donationmatching.entity.AuditRecord;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationPhoto;
import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.Transaction;
import com.sevasahayog.donationmatching.entity.TransactionStatus;
import com.sevasahayog.donationmatching.entity.Urgency;
import com.sevasahayog.donationmatching.entity.User;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtoSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static User donor() {
        return User.builder()
                .id(1L)
                .name("Alice Donor")
                .email("alice@example.com")
                .password("do-not-expose-this-hash")
                .role(Role.DONOR)
                .build();
    }

    @Test
    void userResponseDoesNotExposePassword() throws Exception {
        User user = donor();

        String json = mapper.writeValueAsString(UserResponse.from(user));

        assertThat(json).doesNotContain("password");
        assertThat(json).contains("alice@example.com");
        assertThat(json).contains("DONOR");
    }

    @Test
    void userSummaryResponseExposesOnlyIdAndName() throws Exception {
        String json = mapper.writeValueAsString(UserSummaryResponse.from(donor()));

        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("email");
        assertThat(json).doesNotContain("role");
        assertThat(json).contains("Alice Donor");
    }

    @Test
    void donationResponseDoesNotExposeInternalUserData() throws Exception {
        Donation donation = Donation.builder()
                .id(10L)
                .donor(donor())
                .title("Blankets")
                .description("Ten woollen blankets.")
                .category(Category.HOUSEHOLD)
                .quantity(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES)
                .condition(Condition.GOOD)
                .city("Pune")
                .build();
        DonationPhoto photo = DonationPhoto.builder()
                .id(5L)
                .donation(donation)
                .storageKey("donations/10/blankets.jpg")
                .originalFilename("blankets.jpg")
                .contentType("image/jpeg")
                .fileSize(2048L)
                .build();

        DonationResponse response = DonationResponse.from(donation, List.of(photo));
        String json = mapper.writeValueAsString(response);

        assertThat(json).doesNotContain("password");
        assertThat(json).contains("donations/10/blankets.jpg");
        assertThat(json).contains("SUBMITTED");
    }

    @Test
    void requirementResponseExposesSafeReceiverSummary() throws Exception {
        User receiver = User.builder()
                .id(2L)
                .name("Seva NGO")
                .email("seva@example.com")
                .password("secret-hash")
                .role(Role.RECEIVER)
                .build();
        Requirement requirement = Requirement.builder()
                .id(3L)
                .receiver(receiver)
                .title("Need blankets")
                .description("Warm blankets for residents.")
                .category(Category.HOUSEHOLD)
                .quantityRequired(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES)
                .city("Pune")
                .urgency(Urgency.HIGH)
                .build();

        String json = mapper.writeValueAsString(RequirementResponse.from(requirement));

        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("secret-hash");
    }

    @Test
    void matchResponseCarriesSummariesAndScore() throws Exception {
        Donation donation = Donation.builder()
                .id(10L).donor(donor()).title("Blankets").description("Ten blankets.")
                .category(Category.HOUSEHOLD).quantity(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.GOOD).city("Pune").build();
        User receiver = User.builder().id(2L).name("Seva NGO").email("seva@example.com")
                .password("h").role(Role.RECEIVER).build();
        Requirement requirement = Requirement.builder()
                .id(3L).receiver(receiver).title("Need blankets").description("Need blankets.")
                .category(Category.HOUSEHOLD).quantityRequired(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES).city("Pune").urgency(Urgency.HIGH).build();
        Match match = Match.builder()
                .id(20L).donation(donation).requirement(requirement).score(new BigDecimal("85.50")).build();

        MatchResponse response = MatchResponse.from(match);
        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("85.50");
        assertThat(json).contains("Need blankets");
        assertThat(json).doesNotContain("password");
    }

    @Test
    void transactionResponseExposesMatchIdAndSummaries() throws Exception {
        Donation donation = Donation.builder()
                .id(10L).donor(donor()).title("Blankets").description("Ten blankets.")
                .category(Category.HOUSEHOLD).quantity(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.GOOD).city("Pune").build();
        User receiver = User.builder().id(2L).name("Seva NGO").email("seva@example.com")
                .password("h").role(Role.RECEIVER).build();
        Requirement requirement = Requirement.builder()
                .id(3L).receiver(receiver).title("Need blankets").description("Need blankets.")
                .category(Category.HOUSEHOLD).quantityRequired(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES).city("Pune").urgency(Urgency.HIGH).build();
        Match match = Match.builder().id(20L).donation(donation).requirement(requirement)
                .score(new BigDecimal("85.50")).build();
        Transaction transaction = Transaction.builder()
                .id(30L).match(match).donor(donor()).receiver(receiver)
                .status(TransactionStatus.PENDING).build();

        TransactionResponse response = TransactionResponse.from(transaction);
        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"matchId\":20");
        assertThat(json).doesNotContain("password");
    }

    @Test
    void auditRecordResponseExposesSafeActorSummary() throws Exception {
        AuditRecord record = AuditRecord.builder()
                .id(7L)
                .actor(User.builder().id(1L).name("Alice Donor").email("alice@example.com")
                        .password("hash").role(Role.ADMIN).build())
                .action("APPROVE")
                .entityType("Donation")
                .entityId("10")
                .details("approved by admin")
                .build();

        AuditRecordResponse response = AuditRecordResponse.from(record);
        String json = mapper.writeValueAsString(response);

        assertThat(json).doesNotContain("password");
        assertThat(json).contains("\"entityId\":\"10\"");
    }

    @Test
    void enumValuesDeserializeFromStrings() throws Exception {
        String json = """
                {"title":"Blankets","description":"Ten blankets.","category":"FOOD",
                 "quantity":5,"quantityUnit":"BAGS","condition":"NEW","city":"Pune"}
                """;

        DonationRequest request = mapper.readValue(json, DonationRequest.class);

        assertThat(request.category()).isEqualTo(Category.FOOD);
        assertThat(request.quantityUnit()).isEqualTo(QuantityUnit.BAGS);
        assertThat(request.condition()).isEqualTo(Condition.NEW);
    }

    @Test
    void invalidEnumValueFailsDeserialization() {
        String json = """
                {"title":"Blankets","description":"Ten blankets.","category":"FOOD2",
                 "quantity":5,"quantityUnit":"BAGS","condition":"NEW","city":"Pune"}
                """;

        assertThatThrownBy(() -> mapper.readValue(json, DonationRequest.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void serverControlledFieldsAreIgnoredInRequests() throws Exception {
        String json = """
                {"title":"Blankets","description":"Ten blankets.","category":"FOOD",
                 "quantity":5,"quantityUnit":"BAGS","condition":"NEW","city":"Pune",
                 "status":"APPROVED","donorId":999,"createdAt":"2020-01-01T00:00:00Z"}
                """;

        DonationRequest request = mapper.readValue(json, DonationRequest.class);

        assertThat(request.title()).isEqualTo("Blankets");
        assertThat(request.category()).isEqualTo(Category.FOOD);
        assertThat(DonationRequest.class.getRecordComponents()).extracting("name")
                .doesNotContain("status", "donorId", "createdAt");
    }
}
