package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.RequirementStatus;
import jakarta.validation.constraints.NotNull;

public record RequirementStatusUpdateRequest(
        @NotNull(message = "status must not be null")
        RequirementStatus status
) {
}
