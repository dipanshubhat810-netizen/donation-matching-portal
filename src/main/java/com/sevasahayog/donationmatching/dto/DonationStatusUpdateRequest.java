package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.DonationStatus;
import jakarta.validation.constraints.NotNull;

public record DonationStatusUpdateRequest(
        @NotNull(message = "status must not be null")
        DonationStatus status
) {
}
