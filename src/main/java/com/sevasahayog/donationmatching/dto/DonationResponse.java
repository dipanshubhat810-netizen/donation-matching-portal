package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationPhoto;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.QuantityUnit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DonationResponse(
        long id,
        String title,
        String description,
        Category category,
        BigDecimal quantity,
        QuantityUnit quantityUnit,
        Condition condition,
        String city,
        String locality,
        String pincode,
        DonationStatus status,
        UserSummaryResponse donor,
        List<DonationPhotoResponse> photos,
        Instant createdAt,
        Instant updatedAt
) {
    public static DonationResponse from(Donation donation, List<DonationPhoto> photos) {
        List<DonationPhotoResponse> photoResponses = photos.stream()
                .map(DonationPhotoResponse::from)
                .toList();
        return new DonationResponse(
                donation.getId(),
                donation.getTitle(),
                donation.getDescription(),
                donation.getCategory(),
                donation.getQuantity(),
                donation.getQuantityUnit(),
                donation.getCondition(),
                donation.getCity(),
                donation.getLocality(),
                donation.getPincode(),
                donation.getStatus(),
                UserSummaryResponse.from(donation.getDonor()),
                photoResponses,
                donation.getCreatedAt(),
                donation.getUpdatedAt());
    }

    public static DonationResponse from(Donation donation) {
        return from(donation, List.of());
    }
}
