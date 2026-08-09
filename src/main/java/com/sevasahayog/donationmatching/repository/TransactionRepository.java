package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.Transaction;
import com.sevasahayog.donationmatching.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByMatchId(Long matchId);

    Page<Transaction> findAllByStatus(TransactionStatus status, Pageable pageable);

    List<Transaction> findByDonorId(Long donorId);

    List<Transaction> findByReceiverId(Long receiverId);
}
