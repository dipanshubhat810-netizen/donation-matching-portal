package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.DonationPhoto;

import java.time.Instant;

public record DonationPhotoResponse(
        long id,
        String storageKey,
        String originalFilename,
        String contentType,
        Long fileSize,
        Instant createdAt
) {
    public static DonationPhotoResponse from(DonationPhoto photo) {
        return new DonationPhotoResponse(
                photo.getId(),
                photo.getStorageKey(),
                photo.getOriginalFilename(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getCreatedAt());
    }
}
