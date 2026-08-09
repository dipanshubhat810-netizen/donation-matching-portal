package com.sevasahayog.donationmatching.matching;

import com.sevasahayog.donationmatching.dto.MatchScoreBreakdown;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Urgency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class MatchScorer {

    private static final BigDecimal CATEGORY_SCORE = new BigDecimal("30");
    private static final BigDecimal LOCATION_SCORE = new BigDecimal("20");
    private static final BigDecimal MAX_QUANTITY_SCORE = new BigDecimal("30");
    private static final BigDecimal MAX_TOTAL_SCORE = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;

    public Optional<MatchScoreBreakdown> evaluate(Donation donation, Requirement requirement) {
        if (!passesHardGates(donation, requirement)) {
            return Optional.empty();
        }
        BigDecimal quantityScore = quantityScore(donation.getQuantity(), requirement.getQuantityRequired());
        BigDecimal urgencyScore = new BigDecimal(urgencyScore(requirement.getUrgency()));
        BigDecimal total = CATEGORY_SCORE.add(quantityScore).add(LOCATION_SCORE).add(urgencyScore);
        if (total.compareTo(MAX_TOTAL_SCORE) > 0) {
            total = MAX_TOTAL_SCORE;
        }
        return Optional.of(new MatchScoreBreakdown(
                CATEGORY_SCORE, quantityScore, LOCATION_SCORE, urgencyScore, total.setScale(2, RoundingMode.HALF_UP)));
    }

    public boolean passesHardGates(Donation donation, Requirement requirement) {
        if (donation.getStatus() != DonationStatus.APPROVED) {
            return false;
        }
        if (requirement.getStatus() != RequirementStatus.APPROVED) {
            return false;
        }
        if (donation.getCategory() != requirement.getCategory()) {
            return false;
        }
        if (donation.getQuantityUnit() != requirement.getQuantityUnit()) {
            return false;
        }
        if (donation.getQuantity().compareTo(requirement.getQuantityRequired()) < 0) {
            return false;
        }
        return donation.getCity().trim().equalsIgnoreCase(requirement.getCity().trim());
    }

    private BigDecimal quantityScore(BigDecimal donationQuantity, BigDecimal requirementQuantity) {
        BigDecimal ratio = requirementQuantity.divide(donationQuantity, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(ONE) > 0) {
            ratio = ONE;
        }
        BigDecimal score = ratio.multiply(MAX_QUANTITY_SCORE).setScale(2, RoundingMode.HALF_UP);
        return score.min(MAX_QUANTITY_SCORE);
    }

    private int urgencyScore(Urgency urgency) {
        return switch (urgency) {
            case HIGH -> 20;
            case MEDIUM -> 12;
            case LOW -> 5;
        };
    }
}
