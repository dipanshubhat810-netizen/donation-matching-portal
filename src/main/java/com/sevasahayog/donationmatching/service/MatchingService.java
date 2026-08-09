package com.sevasahayog.donationmatching.service;

import com.sevasahayog.donationmatching.dto.MatchGenerationSummary;
import com.sevasahayog.donationmatching.dto.MatchResponse;
import com.sevasahayog.donationmatching.dto.MatchScoreBreakdown;
import com.sevasahayog.donationmatching.dto.MatchSuggestionResponse;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.exception.InvalidOperationException;
import com.sevasahayog.donationmatching.exception.InvalidStatusTransitionException;
import com.sevasahayog.donationmatching.exception.MatchNotFoundException;
import com.sevasahayog.donationmatching.exception.RequirementNotFoundException;
import com.sevasahayog.donationmatching.matching.MatchScorer;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.MatchRepository;
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final BigDecimal SUGGESTION_THRESHOLD = new BigDecimal("70");
    private static final int TOP_SUGGESTIONS = 5;
    private static final int REQUIREMENT_PAGE_SIZE = 100;

    private final DonationRepository donationRepository;
    private final RequirementRepository requirementRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final MatchScorer matchScorer;
    private final TransactionService transactionService;
    private final AuditService auditService;
    private final TransactionTemplate requiresNewTransaction;

    public MatchingService(DonationRepository donationRepository,
                           RequirementRepository requirementRepository,
                           MatchRepository matchRepository,
                           UserRepository userRepository,
                           MatchScorer matchScorer,
                           TransactionService transactionService,
                           AuditService auditService,
                           PlatformTransactionManager transactionManager) {
        this.donationRepository = donationRepository;
        this.requirementRepository = requirementRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.matchScorer = matchScorer;
        this.transactionService = transactionService;
        this.auditService = auditService;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public List<MatchSuggestionResponse> suggestForRequirement(Long requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RequirementNotFoundException("Requirement not found"));
        return suggestFor(requirement);
    }

    @Transactional
    public MatchGenerationSummary suggestForAll() {
        int requirementsEvaluated = 0;
        int suggestionsCreated = 0;
        int pageNumber = 0;
        Page<Requirement> page;
        do {
            page = requirementRepository.findAllByStatus(RequirementStatus.APPROVED,
                    org.springframework.data.domain.PageRequest.of(pageNumber, REQUIREMENT_PAGE_SIZE));
            for (Requirement requirement : page.getContent()) {
                requirementsEvaluated++;
                suggestionsCreated += suggestFor(requirement).size();
            }
            pageNumber++;
        } while (page.hasNext());
        return new MatchGenerationSummary(requirementsEvaluated, suggestionsCreated);
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> listMatches(MatchStatus status, Pageable pageable) {
        if (status == null) {
            return matchRepository.findAll(pageable).map(MatchResponse::from);
        }
        return matchRepository.findAllByStatus(status, pageable).map(MatchResponse::from);
    }

    @Transactional
    public MatchResponse approve(Long adminId, Long matchId) {
        Match match = findMatch(matchId);
        if (match.getStatus() != MatchStatus.SUGGESTED) {
            throw new InvalidStatusTransitionException(
                    "Match cannot transition from " + match.getStatus() + " to APPROVED");
        }
        Donation donation = match.getDonation();
        if (donation.getStatus() != DonationStatus.APPROVED) {
            throw new InvalidOperationException(
                    "Donation " + donation.getId() + " is no longer available for matching");
        }
        Requirement requirement = match.getRequirement();
        if (requirement.getStatus() != RequirementStatus.APPROVED) {
            throw new InvalidOperationException(
                    "Requirement " + requirement.getId() + " is no longer available for matching");
        }
        match.setStatus(MatchStatus.APPROVED);
        match.setReviewedAt(Instant.now());
        match.setReviewedBy(findAdmin(adminId));
        donation.setStatus(DonationStatus.MATCHED);
        transactionService.createForMatch(match);
        auditService.record(adminId, "MATCH_APPROVED", "Match",
                String.valueOf(matchId),
                "Match " + matchId + " approved; transaction created");
        return MatchResponse.from(match);
    }

    @Transactional
    public MatchResponse reject(Long adminId, Long matchId) {
        Match match = findMatch(matchId);
        if (match.getStatus() != MatchStatus.SUGGESTED) {
            throw new InvalidStatusTransitionException(
                    "Match cannot transition from " + match.getStatus() + " to REJECTED");
        }
        match.setStatus(MatchStatus.REJECTED);
        match.setReviewedAt(Instant.now());
        match.setReviewedBy(findAdmin(adminId));
        auditService.record(adminId, "MATCH_REJECTED", "Match",
                String.valueOf(matchId),
                "Match " + matchId + " rejected");
        return MatchResponse.from(match);
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found"));
    }

    private User findAdmin(Long adminId) {
        return userRepository.findById(adminId).orElse(null);
    }

    private List<MatchSuggestionResponse> suggestFor(Requirement requirement) {
        if (requirement.getStatus() != RequirementStatus.APPROVED) {
            return List.of();
        }
        Set<Long> alreadyMatchedDonationIds = matchRepository.findByRequirementId(requirement.getId())
                .stream()
                .map(match -> match.getDonation().getId())
                .collect(Collectors.toSet());

        List<ScoredCandidate> ranked = donationRepository
                .findAllByStatusAndCategoryAndQuantityUnitAndCityIgnoreCase(
                        DonationStatus.APPROVED,
                        requirement.getCategory(),
                        requirement.getQuantityUnit(),
                        requirement.getCity().trim())
                .stream()
                .filter(donation -> !alreadyMatchedDonationIds.contains(donation.getId()))
                .map(donation -> matchScorer.evaluate(donation, requirement)
                        .map(breakdown -> new ScoredCandidate(donation, breakdown)))
                .flatMap(java.util.Optional::stream)
                .filter(candidate -> candidate.breakdown().totalScore().compareTo(SUGGESTION_THRESHOLD) >= 0)
                .sorted(ScoredCandidate.COMPARATOR)
                .limit(TOP_SUGGESTIONS)
                .toList();

        List<MatchSuggestionResponse> created = new ArrayList<>();
        for (ScoredCandidate candidate : ranked) {
            Match saved = insertIfAbsent(candidate.donation(), requirement, candidate.breakdown());
            if (saved != null) {
                created.add(MatchSuggestionResponse.from(saved, candidate.breakdown()));
            }
        }
        return created;
    }

    private Match insertIfAbsent(Donation donation, Requirement requirement, MatchScoreBreakdown breakdown) {
        try {
            return requiresNewTransaction.execute(status -> {
                if (matchRepository.existsByDonationIdAndRequirementId(donation.getId(), requirement.getId())) {
                    return null;
                }
                Match match = Match.builder()
                        .donation(donation)
                        .requirement(requirement)
                        .score(breakdown.totalScore())
                        .status(MatchStatus.SUGGESTED)
                        .build();
                return matchRepository.saveAndFlush(match);
            });
        } catch (DataIntegrityViolationException e) {
            return null;
        }
    }

    private record ScoredCandidate(Donation donation, MatchScoreBreakdown breakdown) {

        private static final Comparator<ScoredCandidate> COMPARATOR =
                Comparator.comparing(ScoredCandidate::breakdown,
                                Comparator.comparing(MatchScoreBreakdown::totalScore).reversed())
                        .thenComparing(candidate -> candidate.donation().getId());
    }

}
