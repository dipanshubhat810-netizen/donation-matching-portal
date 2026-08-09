package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.AuditRecord;

import java.time.Instant;

public record AuditRecordResponse(
        long id,
        String entityType,
        String entityId,
        String action,
        UserSummaryResponse actor,
        String details,
        Instant createdAt
) {
    public static AuditRecordResponse from(AuditRecord record) {
        return new AuditRecordResponse(
                record.getId(),
                record.getEntityType(),
                record.getEntityId(),
                record.getAction(),
                record.getActor() != null ? UserSummaryResponse.from(record.getActor()) : null,
                record.getDetails(),
                record.getCreatedAt());
    }
}
