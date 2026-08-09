package com.sevasahayog.donationmatching.controller;

import com.sevasahayog.donationmatching.dto.MatchGenerationSummary;
import com.sevasahayog.donationmatching.dto.MatchResponse;
import com.sevasahayog.donationmatching.dto.MatchSuggestionResponse;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import com.sevasahayog.donationmatching.service.MatchingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AdminController(MatchingService matchingService) {
        this.matchingService = matchingService;
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
}
