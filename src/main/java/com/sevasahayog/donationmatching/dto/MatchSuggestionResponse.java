package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record MatchSuggestionResponse(
        long id,
        DonationSummaryResponse donation,
        RequirementSummaryResponse requirement,
        BigDecimal score,
        MatchStatus status,
        MatchScoreBreakdown breakdown,
        Instant createdAt,
        Instant updatedAt
) {
    public static MatchSuggestionResponse from(Match match, MatchScoreBreakdown breakdown) {
        return new MatchSuggestionResponse(
                match.getId(),
                DonationSummaryResponse.from(match.getDonation()),
                RequirementSummaryResponse.from(match.getRequirement()),
                match.getScore(),
                match.getStatus(),
                breakdown,
                match.getCreatedAt(),
                match.getUpdatedAt());
    }
}
