package com.sevasahayog.donationmatching.dto;

import java.math.BigDecimal;

public record MatchScoreBreakdown(
        BigDecimal categoryScore,
        BigDecimal quantityScore,
        BigDecimal locationScore,
        BigDecimal urgencyScore,
        BigDecimal totalScore
) {
    public String explanation() {
        return "Category " + categoryScore + " + Quantity " + quantityScore
                + " + Location " + locationScore + " + Urgency " + urgencyScore
                + " = " + totalScore;
    }
}
