package com.sevasahayog.donationmatching.controller;

import com.sevasahayog.donationmatching.dto.DonationRequest;
import com.sevasahayog.donationmatching.dto.DonationResponse;
import com.sevasahayog.donationmatching.dto.DonationStatusUpdateRequest;
import com.sevasahayog.donationmatching.dto.DonationUpdateRequest;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.security.UserPrincipal;
import com.sevasahayog.donationmatching.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DONOR')")
    public DonationResponse create(@Valid @RequestBody DonationRequest request,
                                   Authentication authentication) {
        return donationService.create(principalId(authentication), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DONOR')")
    public Page<DonationResponse> my(@PageableDefault(size = 20) Pageable pageable,
                                     Authentication authentication) {
        return donationService.listMine(principalId(authentication), pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('RECEIVER') or hasRole('ADMIN')")
    public Page<DonationResponse> search(@RequestParam(required = false) Category category,
                                         @RequestParam(required = false) String city,
                                         @RequestParam(required = false) String query,
                                         @RequestParam(required = false) DonationStatus status,
                                         @PageableDefault(size = 20, sort = "createdAt",
                                                 direction = Sort.Direction.DESC) Pageable pageable,
                                         Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return donationService.search(principal.getRole(), category, city, query, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DONOR') or hasRole('ADMIN')")
    public DonationResponse get(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return donationService.getForReading(id, principal.getId(), principal.getRole());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody DonationUpdateRequest request,
                                       Authentication authentication) {
        donationService.update(principalId(authentication), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public DonationResponse updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody DonationStatusUpdateRequest request,
                                         Authentication authentication) {
        return donationService.updateStatus(principalId(authentication), id, request.status());
    }

    private Long principalId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
