package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;

public record DonationSummaryResponse(
        long id,
        String title,
        String city,
        DonationStatus status
) {
    public static DonationSummaryResponse from(Donation donation) {
        return new DonationSummaryResponse(
                donation.getId(),
                donation.getTitle(),
                donation.getCity(),
                donation.getStatus());
    }
}
