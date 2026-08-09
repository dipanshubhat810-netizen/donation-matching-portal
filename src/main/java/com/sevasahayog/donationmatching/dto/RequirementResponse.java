package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Urgency;

import java.math.BigDecimal;
import java.time.Instant;

public record RequirementResponse(
        long id,
        String title,
        String description,
        Category category,
        BigDecimal quantity,
        QuantityUnit quantityUnit,
        String city,
        String locality,
        String pincode,
        Urgency urgency,
        RequirementStatus status,
        UserSummaryResponse receiver,
        Instant createdAt,
        Instant updatedAt
) {
    public static RequirementResponse from(Requirement requirement) {
        return new RequirementResponse(
                requirement.getId(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getCategory(),
                requirement.getQuantityRequired(),
                requirement.getQuantityUnit(),
                requirement.getCity(),
                requirement.getLocality(),
                requirement.getPincode(),
                requirement.getUrgency(),
                requirement.getStatus(),
                UserSummaryResponse.from(requirement.getReceiver()),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt());
    }
}
