package com.sevasahayog.donationmatching.service;

import com.sevasahayog.donationmatching.entity.AuditRecord;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.AuditRecordRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditRecordRepository auditRecordRepository;
    private final UserRepository userRepository;

    public AuditService(AuditRecordRepository auditRecordRepository, UserRepository userRepository) {
        this.auditRecordRepository = auditRecordRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(Long actorId, String action, String entityType, String entityId, String details) {
        User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
        auditRecordRepository.save(AuditRecord.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
