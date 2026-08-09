package com.sevasahayog.donationmatching.dto;

public record MatchGenerationSummary(
        int requirementsEvaluated,
        int suggestionsCreated
) {
}
