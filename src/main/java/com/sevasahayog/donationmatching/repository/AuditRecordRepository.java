package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<AuditRecord> findByActorId(Long actorId);

    List<AuditRecord> findAllByOrderByCreatedAtDesc();
}
