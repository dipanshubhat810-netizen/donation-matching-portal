package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.dto.DonationRequest;
import com.sevasahayog.donationmatching.dto.RequirementRequest;
import com.sevasahayog.donationmatching.dto.UserRegisterRequest;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Urgency;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static void assertNoViolations(Object dto) {
        assertThat(validator.validate(dto)).isEmpty();
    }

    private static void assertViolation(Object dto, String property) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        assertThat(violations)
                .as("expected a violation on '%s' for %s but found %s", property, dto, violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals(property));
    }

    private static UserRegisterRequest validUserRegistration() {
        return new UserRegisterRequest("Jane Doe", "jane@example.com", "password123");
    }

    private static DonationRequest validDonation() {
        return new DonationRequest(
                "Winter blankets",
                "Ten woollen blankets in good condition.",
                Category.HOUSEHOLD,
                new BigDecimal("10"),
                QuantityUnit.PIECES,
                Condition.GOOD,
                "Pune",
                "Kothrud",
                "411038");
    }

    private static RequirementRequest validRequirement() {
        return new RequirementRequest(
                "Need blankets",
                "Old age home needs warm blankets.",
                Category.HOUSEHOLD,
                new BigDecimal("10"),
                QuantityUnit.PIECES,
                "Pune",
                null,
                null,
                Urgency.HIGH);
    }

    @Test
    void userRegistrationAcceptsValidRequest() {
        assertNoViolations(validUserRegistration());
        assertNoViolations(new UserRegisterRequest("Jane Doe", "jane@example.com", "password"));
        assertNoViolations(new UserRegisterRequest("Jane Doe", "jane@example.com", "p".repeat(72)));
    }

    @Test
    void userRegistrationRejectsBlankName() {
        assertViolation(new UserRegisterRequest("   ", "jane@example.com", "password123"), "name");
    }

    @Test
    void userRegistrationRejectsOversizedName() {
        assertViolation(new UserRegisterRequest("n".repeat(101), "jane@example.com", "password123"), "name");
    }

    @Test
    void userRegistrationRejectsBlankEmail() {
        assertViolation(new UserRegisterRequest("Jane Doe", "  ", "password123"), "email");
    }

    @Test
    void userRegistrationRejectsInvalidEmail() {
        assertViolation(new UserRegisterRequest("Jane Doe", "not-an-email", "password123"), "email");
    }

    @Test
    void userRegistrationRejectsOversizedEmail() {
        assertViolation(new UserRegisterRequest("Jane Doe", "a".repeat(250) + "@example.com", "password123"), "email");
    }

    @Test
    void userRegistrationRejectsBlankPassword() {
        assertViolation(new UserRegisterRequest("Jane Doe", "jane@example.com", "   "), "password");
    }

    @Test
    void userRegistrationRejectsShortPassword() {
        assertViolation(new UserRegisterRequest("Jane Doe", "jane@example.com", "p".repeat(7)), "password");
    }

    @Test
    void userRegistrationRejectsLongPassword() {
        assertViolation(new UserRegisterRequest("Jane Doe", "jane@example.com", "p".repeat(73)), "password");
    }

    @Test
    void donationAcceptsValidRequest() {
        assertNoViolations(validDonation());
    }

    @Test
    void donationRejectsBlankTitle() {
        assertViolation(new DonationRequest("   ", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "title");
    }

    @Test
    void donationRejectsOversizedTitle() {
        assertViolation(new DonationRequest("t".repeat(201), "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "title");
    }

    @Test
    void donationRejectsBlankDescription() {
        assertViolation(new DonationRequest("Blankets", "   ", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "description");
    }

    @Test
    void donationRejectsNullCategory() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", null,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "category");
    }

    @Test
    void donationRejectsNullQuantity() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                null, QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "quantity");
    }

    @Test
    void donationRejectsZeroQuantity() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                BigDecimal.ZERO, QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "quantity");
    }

    @Test
    void donationRejectsNegativeQuantity() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("-1"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "quantity");
    }

    @Test
    void donationRejectsExcessDecimalPrecision() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10.1234"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "quantity");
    }

    @Test
    void donationRejectsExcessIntegerDigits() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10000000000"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null), "quantity");
    }

    @Test
    void donationRejectsNullUnit() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), null, Condition.GOOD, "Pune", null, null), "quantityUnit");
    }

    @Test
    void donationRejectsNullCondition() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, null, "Pune", null, null), "condition");
    }

    @Test
    void donationRejectsBlankCity() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, " ", null, null), "city");
    }

    @Test
    void donationRejectsOversizedLocality() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", "l".repeat(101), null), "locality");
    }

    @Test
    void donationRejectsOversizedPincode() {
        assertViolation(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, "1".repeat(21)), "pincode");
    }

    @Test
    void donationAcceptsOptionalFieldsOmitted() {
        assertNoViolations(new DonationRequest("Blankets", "Ten woollen blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, Condition.GOOD, "Pune", null, null));
    }

    @Test
    void requirementAcceptsValidRequest() {
        assertNoViolations(validRequirement());
    }

    @Test
    void requirementRejectsBlankTitle() {
        assertViolation(new RequirementRequest("  ", "Old age home needs blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, "Pune", null, null, Urgency.HIGH), "title");
    }

    @Test
    void requirementRejectsNullCategory() {
        assertViolation(new RequirementRequest("Need blankets", "Old age home needs blankets.", null,
                new BigDecimal("10"), QuantityUnit.PIECES, "Pune", null, null, Urgency.HIGH), "category");
    }

    @Test
    void requirementRejectsZeroQuantity() {
        assertViolation(new RequirementRequest("Need blankets", "Old age home needs blankets.", Category.HOUSEHOLD,
                BigDecimal.ZERO, QuantityUnit.PIECES, "Pune", null, null, Urgency.HIGH), "quantity");
    }

    @Test
    void requirementRejectsNullUnit() {
        assertViolation(new RequirementRequest("Need blankets", "Old age home needs blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), null, "Pune", null, null, Urgency.HIGH), "quantityUnit");
    }

    @Test
    void requirementRejectsNullUrgency() {
        assertViolation(new RequirementRequest("Need blankets", "Old age home needs blankets.", Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, "Pune", null, null, null), "urgency");
    }

    @Test
    void requirementRejectsOversizedDescription() {
        assertViolation(new RequirementRequest("Need blankets", "d".repeat(2001), Category.HOUSEHOLD,
                new BigDecimal("10"), QuantityUnit.PIECES, "Pune", null, null, Urgency.HIGH), "description");
    }
}
