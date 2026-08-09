package com.sevasahayog.donationmatching.service;

import com.sevasahayog.donationmatching.dto.RequirementRequest;
import com.sevasahayog.donationmatching.dto.RequirementResponse;
import com.sevasahayog.donationmatching.dto.RequirementUpdateRequest;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.exception.ForbiddenException;
import com.sevasahayog.donationmatching.exception.InvalidOperationException;
import com.sevasahayog.donationmatching.exception.InvalidStatusTransitionException;
import com.sevasahayog.donationmatching.exception.RequirementNotFoundException;
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.RequirementSpecifications;
import com.sevasahayog.donationmatching.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class RequirementService {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt");

    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RequirementService(RequirementRepository requirementRepository,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RequirementResponse create(Long receiverId, RequirementRequest request) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new InvalidOperationException("Authenticated receiver account not found"));
        Requirement requirement = Requirement.builder()
                .receiver(receiver)
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .quantityRequired(request.quantity())
                .quantityUnit(request.quantityUnit())
                .city(request.city())
                .locality(request.locality())
                .pincode(request.pincode())
                .urgency(request.urgency())
                .status(RequirementStatus.SUBMITTED)
                .build();
        return RequirementResponse.from(requirementRepository.save(requirement));
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> listMine(Long receiverId, Pageable pageable) {
        return requirementRepository.findAllByReceiverId(receiverId, pageable)
                .map(RequirementResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> search(Role role, Category category, String city, String query,
                                            RequirementStatus status, Pageable pageable) {
        if (role == Role.RECEIVER) {
            throw new ForbiddenException("Receivers cannot search requirements");
        }
        if (role == Role.DONOR) {
            if (status != null) {
                throw new InvalidOperationException(
                        "status filtering is not supported for this search; available requirements are always APPROVED");
            }
            status = RequirementStatus.APPROVED;
        }
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new InvalidOperationException("query must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        validateSort(pageable);
        Specification<Requirement> specification = buildSearchSpecification(status, category, city, query);
        return requirementRepository.findAll(specification, pageable)
                .map(RequirementResponse::from);
    }

    private Specification<Requirement> buildSearchSpecification(RequirementStatus status, Category category,
                                                                String city, String query) {
        Specification<Requirement> specification = (root, q, cb) -> cb.conjunction();
        if (status != null) {
            specification = specification.and(RequirementSpecifications.statusIs(status));
        }
        if (category != null) {
            specification = specification.and(RequirementSpecifications.categoryIs(category));
        }
        if (city != null && !city.isBlank()) {
            specification = specification.and(RequirementSpecifications.cityEquals(city.trim()));
        }
        if (query != null && !query.isBlank()) {
            specification = specification.and(RequirementSpecifications.textMatches(query.trim()));
        }
        return specification;
    }

    private void validateSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new InvalidOperationException("Unsupported sort property: " + order.getProperty());
            }
        }
    }

    @Transactional(readOnly = true)
    public RequirementResponse getForReading(Long requirementId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return RequirementResponse.from(requirementRepository.findById(requirementId)
                    .orElseThrow(() -> new RequirementNotFoundException("Requirement not found")));
        }
        return RequirementResponse.from(findOwnedBy(userId, requirementId));
    }

    @Transactional
    public void update(Long receiverId, Long requirementId, RequirementUpdateRequest request) {
        Requirement requirement = findOwnedBy(receiverId, requirementId);
        if (requirement.getStatus() != RequirementStatus.SUBMITTED) {
            throw new InvalidOperationException("Requirement can only be edited while its status is SUBMITTED");
        }
        applyUpdate(requirement, request);
    }

    @Transactional
    public RequirementResponse updateStatus(Long adminId, Long requirementId, RequirementStatus target) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RequirementNotFoundException("Requirement not found"));
        RequirementStatus current = requirement.getStatus();
        boolean allowed = current == RequirementStatus.SUBMITTED
                && (target == RequirementStatus.APPROVED || target == RequirementStatus.REJECTED);
        if (!allowed) {
            throw new InvalidStatusTransitionException(
                    "Requirement cannot transition from " + current + " to " + target);
        }
        requirement.setStatus(target);
        auditService.record(adminId, "REQUIREMENT_" + target.name(), "Requirement",
                String.valueOf(requirementId),
                "Requirement " + requirementId + " transitioned from " + current + " to " + target);
        return RequirementResponse.from(requirement);
    }

    private Requirement findOwnedBy(Long receiverId, Long requirementId) {
        return requirementRepository.findByIdAndReceiverId(requirementId, receiverId)
                .orElseThrow(() -> new ForbiddenException("Requirement not accessible"));
    }

    private void applyUpdate(Requirement requirement, RequirementUpdateRequest request) {
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new InvalidOperationException("title must not be blank");
            }
            requirement.setTitle(request.title());
        }
        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw new InvalidOperationException("description must not be blank");
            }
            requirement.setDescription(request.description());
        }
        if (request.category() != null) {
            requirement.setCategory(request.category());
        }
        if (request.quantity() != null) {
            requirement.setQuantityRequired(request.quantity());
        }
        if (request.quantityUnit() != null) {
            requirement.setQuantityUnit(request.quantityUnit());
        }
        if (request.city() != null) {
            if (request.city().isBlank()) {
                throw new InvalidOperationException("city must not be blank");
            }
            requirement.setCity(request.city());
        }
        if (request.locality() != null) {
            requirement.setLocality(request.locality());
        }
        if (request.pincode() != null) {
            requirement.setPincode(request.pincode());
        }
        if (request.urgency() != null) {
            requirement.setUrgency(request.urgency());
        }
    }
}
