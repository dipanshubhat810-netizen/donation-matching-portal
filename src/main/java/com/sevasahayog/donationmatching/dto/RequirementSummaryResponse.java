package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;

public record RequirementSummaryResponse(
        long id,
        String title,
        String city,
        RequirementStatus status
) {
    public static RequirementSummaryResponse from(Requirement requirement) {
        return new RequirementSummaryResponse(
                requirement.getId(),
                requirement.getTitle(),
                requirement.getCity(),
                requirement.getStatus());
    }
}
