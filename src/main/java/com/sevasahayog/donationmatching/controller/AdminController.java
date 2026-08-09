package com.sevasahayog.donationmatching.controller;

import com.sevasahayog.donationmatching.dto.MatchGenerationSummary;
import com.sevasahayog.donationmatching.dto.MatchResponse;
import com.sevasahayog.donationmatching.dto.MatchSuggestionResponse;
import com.sevasahayog.donationmatching.dto.TransactionResponse;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import com.sevasahayog.donationmatching.entity.TransactionStatus;
import com.sevasahayog.donationmatching.security.UserPrincipal;
import com.sevasahayog.donationmatching.service.MatchingService;
import com.sevasahayog.donationmatching.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MatchingService matchingService;
    private final TransactionService transactionService;

    public AdminController(MatchingService matchingService,
                           TransactionService transactionService) {
        this.matchingService = matchingService;
        this.transactionService = transactionService;
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reviewQueue() {
        return Map.of("status", "ok");
    }

    @PostMapping("/matches/suggest")
    @PreAuthorize("hasRole('ADMIN')")
    public Object suggest(@RequestParam(required = false) Long requirementId) {
        if (requirementId != null) {
            return matchingService.suggestForRequirement(requirementId);
        }
        return matchingService.suggestForAll();
    }

    @GetMapping("/matches")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<MatchResponse> listMatches(@RequestParam(required = false) MatchStatus status,
                                           @PageableDefault(size = 20, sort = "createdAt",
                                                   direction = Sort.Direction.DESC) Pageable pageable) {
        return matchingService.listMatches(status, pageable);
    }

    @PostMapping("/matches/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse approve(@PathVariable Long id, Authentication authentication) {
        return matchingService.approve(principalId(authentication), id);
    }

    @PostMapping("/matches/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse reject(@PathVariable Long id, Authentication authentication) {
        return matchingService.reject(principalId(authentication), id);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<TransactionResponse> listTransactions(@RequestParam(required = false) TransactionStatus status,
                                                      @PageableDefault(size = 20, sort = "createdAt",
                                                              direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.list(status, pageable);
    }

    @PostMapping("/transactions/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionResponse start(@PathVariable Long id, Authentication authentication) {
        return transactionService.start(principalId(authentication), id);
    }

    @PostMapping("/transactions/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionResponse complete(@PathVariable Long id, Authentication authentication) {
        return transactionService.complete(principalId(authentication), id);
    }

    @PostMapping("/transactions/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public TransactionResponse cancel(@PathVariable Long id, Authentication authentication) {
        return transactionService.cancel(principalId(authentication), id);
    }

    private Long principalId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
