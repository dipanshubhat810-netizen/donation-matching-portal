package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Transaction;
import com.sevasahayog.donationmatching.entity.TransactionStatus;

import java.time.Instant;

public record TransactionResponse(
        long id,
        long matchId,
        UserSummaryResponse donor,
        UserSummaryResponse receiver,
        TransactionStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getMatch().getId(),
                UserSummaryResponse.from(transaction.getDonor()),
                UserSummaryResponse.from(transaction.getReceiver()),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getCompletedAt());
    }
}
