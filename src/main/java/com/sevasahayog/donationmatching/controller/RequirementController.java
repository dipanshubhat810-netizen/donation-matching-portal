package com.sevasahayog.donationmatching.controller;

import com.sevasahayog.donationmatching.dto.RequirementRequest;
import com.sevasahayog.donationmatching.dto.RequirementResponse;
import com.sevasahayog.donationmatching.dto.RequirementStatusUpdateRequest;
import com.sevasahayog.donationmatching.dto.RequirementUpdateRequest;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.security.UserPrincipal;
import com.sevasahayog.donationmatching.service.RequirementService;
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
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEIVER')")
    public RequirementResponse create(@Valid @RequestBody RequirementRequest request,
                                      Authentication authentication) {
        return requirementService.create(principalId(authentication), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RECEIVER')")
    public Page<RequirementResponse> my(@PageableDefault(size = 20) Pageable pageable,
                                        Authentication authentication) {
        return requirementService.listMine(principalId(authentication), pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('DONOR') or hasRole('ADMIN')")
    public Page<RequirementResponse> search(@RequestParam(required = false) Category category,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) String query,
                                            @RequestParam(required = false) RequirementStatus status,
                                            @PageableDefault(size = 20, sort = "createdAt",
                                                    direction = Sort.Direction.DESC) Pageable pageable,
                                            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return requirementService.search(principal.getRole(), category, city, query, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RECEIVER') or hasRole('ADMIN')")
    public RequirementResponse get(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return requirementService.getForReading(id, principal.getId(), principal.getRole());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('RECEIVER')")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody RequirementUpdateRequest request,
                                       Authentication authentication) {
        requirementService.update(principalId(authentication), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public RequirementResponse updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody RequirementStatusUpdateRequest request,
                                            Authentication authentication) {
        return requirementService.updateStatus(principalId(authentication), id, request.status());
    }

    private Long principalId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
