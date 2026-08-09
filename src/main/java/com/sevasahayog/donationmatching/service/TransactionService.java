package com.sevasahayog.donationmatching.service;

import com.sevasahayog.donationmatching.dto.TransactionResponse;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Transaction;
import com.sevasahayog.donationmatching.entity.TransactionStatus;
import com.sevasahayog.donationmatching.exception.InvalidStatusTransitionException;
import com.sevasahayog.donationmatching.exception.TransactionNotFoundException;
import com.sevasahayog.donationmatching.repository.MatchRepository;
import com.sevasahayog.donationmatching.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MatchRepository matchRepository;
    private final AuditService auditService;

    public TransactionService(TransactionRepository transactionRepository,
                              MatchRepository matchRepository,
                              AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.matchRepository = matchRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Transaction createForMatch(Match match) {
        return transactionRepository.save(Transaction.builder()
                .match(match)
                .donor(match.getDonation().getDonor())
                .receiver(match.getRequirement().getReceiver())
                .status(TransactionStatus.PENDING)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(TransactionStatus status, Pageable pageable) {
        if (status == null) {
            return transactionRepository.findAll(pageable).map(TransactionResponse::from);
        }
        return transactionRepository.findAllByStatus(status, pageable).map(TransactionResponse::from);
    }

    @Transactional
    public TransactionResponse start(Long adminId, Long transactionId) {
        Transaction transaction = findTransaction(transactionId);
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Transaction cannot transition from " + transaction.getStatus() + " to IN_PROGRESS");
        }
        Match match = transaction.getMatch();
        if (match.getStatus() != MatchStatus.APPROVED) {
            throw new InvalidStatusTransitionException(
                    "Match cannot move to IN_FULFILMENT from " + match.getStatus());
        }
        transaction.setStatus(TransactionStatus.IN_PROGRESS);
        match.setStatus(MatchStatus.IN_FULFILMENT);
        Donation donation = match.getDonation();
        donation.setStatus(DonationStatus.IN_FULFILMENT);
        auditService.record(adminId, "TRANSACTION_STARTED", "Transaction",
                String.valueOf(transactionId),
                "Transaction " + transactionId + " moved to IN_PROGRESS for match " + match.getId());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse complete(Long adminId, Long transactionId) {
        Transaction transaction = findTransaction(transactionId);
        if (transaction.getStatus() != TransactionStatus.IN_PROGRESS) {
            throw new InvalidStatusTransitionException(
                    "Transaction cannot transition from " + transaction.getStatus() + " to COMPLETED");
        }
        Match match = transaction.getMatch();
        if (match.getStatus() != MatchStatus.IN_FULFILMENT) {
            throw new InvalidStatusTransitionException(
                    "Match cannot move to COMPLETED from " + match.getStatus());
        }
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());
        match.setStatus(MatchStatus.COMPLETED);
        match.getDonation().setStatus(DonationStatus.COMPLETED);
        match.getRequirement().setStatus(RequirementStatus.FULFILLED);
        auditService.record(adminId, "TRANSACTION_COMPLETED", "Transaction",
                String.valueOf(transactionId),
                "Transaction " + transactionId + " completed for match " + match.getId());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse cancel(Long adminId, Long transactionId) {
        Transaction transaction = findTransaction(transactionId);
        TransactionStatus current = transaction.getStatus();
        if (current == TransactionStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("A completed transaction cannot be cancelled");
        }
        if (current == TransactionStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Transaction is already cancelled");
        }
        Match match = transaction.getMatch();
        transaction.setStatus(TransactionStatus.CANCELLED);
        match.setStatus(MatchStatus.CANCELLED);
        match.getDonation().setStatus(DonationStatus.APPROVED);
        auditService.record(adminId, "TRANSACTION_CANCELLED", "Transaction",
                String.valueOf(transactionId),
                "Transaction " + transactionId + " cancelled for match " + match.getId());
        return TransactionResponse.from(transaction);
    }

    private Transaction findTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }
}
