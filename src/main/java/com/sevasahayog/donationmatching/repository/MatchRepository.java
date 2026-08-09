package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByDonationId(Long donationId);

    List<Match> findByRequirementId(Long requirementId);

    List<Match> findByDonationIdAndStatus(Long donationId, MatchStatus status);

    List<Match> findByRequirementIdAndStatus(Long requirementId, MatchStatus status);

    boolean existsByDonationIdAndRequirementId(Long donationId, Long requirementId);

    List<Match> findTop5ByRequirementIdAndStatusOrderByScoreDesc(Long requirementId, MatchStatus status);

    Page<Match> findAllByStatus(MatchStatus status, Pageable pageable);
}
