package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record MatchResponse(
        long id,
        DonationSummaryResponse donation,
        RequirementSummaryResponse requirement,
        BigDecimal score,
        MatchStatus status,
        Instant reviewedAt,
        UserSummaryResponse reviewedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                DonationSummaryResponse.from(match.getDonation()),
                RequirementSummaryResponse.from(match.getRequirement()),
                match.getScore(),
                match.getStatus(),
                match.getReviewedAt(),
                match.getReviewedBy() != null ? UserSummaryResponse.from(match.getReviewedBy()) : null,
                match.getCreatedAt(),
                match.getUpdatedAt());
    }
}
