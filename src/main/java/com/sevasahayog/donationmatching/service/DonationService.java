package com.sevasahayog.donationmatching.service;

import com.sevasahayog.donationmatching.dto.DonationRequest;
import com.sevasahayog.donationmatching.dto.DonationResponse;
import com.sevasahayog.donationmatching.dto.DonationUpdateRequest;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.exception.DonationNotFoundException;
import com.sevasahayog.donationmatching.exception.ForbiddenException;
import com.sevasahayog.donationmatching.exception.InvalidOperationException;
import com.sevasahayog.donationmatching.exception.InvalidStatusTransitionException;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.DonationSpecifications;
import com.sevasahayog.donationmatching.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class DonationService {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt");

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DonationService(DonationRepository donationRepository,
                           UserRepository userRepository,
                           AuditService auditService) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DonationResponse create(Long donorId, DonationRequest request) {
        User donor = userRepository.findById(donorId)
                .orElseThrow(() -> new InvalidOperationException("Authenticated donor account not found"));
        Donation donation = Donation.builder()
                .donor(donor)
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .quantity(request.quantity())
                .quantityUnit(request.quantityUnit())
                .condition(request.condition())
                .city(request.city())
                .locality(request.locality())
                .pincode(request.pincode())
                .status(DonationStatus.SUBMITTED)
                .build();
        return DonationResponse.from(donationRepository.save(donation));
    }

    @Transactional(readOnly = true)
    public Page<DonationResponse> listMine(Long donorId, Pageable pageable) {
        return donationRepository.findAllByDonorId(donorId, pageable)
                .map(DonationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DonationResponse> search(Role role, Category category, String city, String query,
                                         DonationStatus status, Pageable pageable) {
        if (role == Role.DONOR) {
            throw new ForbiddenException("Donors cannot search donations");
        }
        if (role == Role.RECEIVER) {
            if (status != null) {
                throw new InvalidOperationException(
                        "status filtering is not supported for this search; available donations are always APPROVED");
            }
            status = DonationStatus.APPROVED;
        }
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new InvalidOperationException("query must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        validateSort(pageable);
        Specification<Donation> specification = buildSearchSpecification(status, category, city, query);
        return donationRepository.findAll(specification, pageable)
                .map(DonationResponse::from);
    }

    private Specification<Donation> buildSearchSpecification(DonationStatus status, Category category,
                                                             String city, String query) {
        Specification<Donation> specification = (root, q, cb) -> cb.conjunction();
        if (status != null) {
            specification = specification.and(DonationSpecifications.statusIs(status));
        }
        if (category != null) {
            specification = specification.and(DonationSpecifications.categoryIs(category));
        }
        if (city != null && !city.isBlank()) {
            specification = specification.and(DonationSpecifications.cityEquals(city.trim()));
        }
        if (query != null && !query.isBlank()) {
            specification = specification.and(DonationSpecifications.textMatches(query.trim()));
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
    public DonationResponse getForReading(Long donationId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return DonationResponse.from(donationRepository.findById(donationId)
                    .orElseThrow(() -> new DonationNotFoundException("Donation not found")));
        }
        return DonationResponse.from(findOwnedBy(userId, donationId));
    }

    @Transactional
    public void update(Long donorId, Long donationId, DonationUpdateRequest request) {
        Donation donation = findOwnedBy(donorId, donationId);
        if (donation.getStatus() != DonationStatus.SUBMITTED) {
            throw new InvalidOperationException("Donation can only be edited while its status is SUBMITTED");
        }
        applyUpdate(donation, request);
    }

    @Transactional
    public DonationResponse updateStatus(Long adminId, Long donationId, DonationStatus target) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new DonationNotFoundException("Donation not found"));
        DonationStatus current = donation.getStatus();
        boolean allowed = current == DonationStatus.SUBMITTED
                && (target == DonationStatus.APPROVED || target == DonationStatus.REJECTED);
        if (!allowed) {
            throw new InvalidStatusTransitionException(
                    "Donation cannot transition from " + current + " to " + target);
        }
        donation.setStatus(target);
        auditService.record(adminId, "DONATION_" + target.name(), "Donation",
                String.valueOf(donationId),
                "Donation " + donationId + " transitioned from " + current + " to " + target);
        return DonationResponse.from(donation);
    }

    private Donation findOwnedBy(Long donorId, Long donationId) {
        return donationRepository.findByIdAndDonorId(donationId, donorId)
                .orElseThrow(() -> new ForbiddenException("Donation not accessible"));
    }

    private void applyUpdate(Donation donation, DonationUpdateRequest request) {
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new InvalidOperationException("title must not be blank");
            }
            donation.setTitle(request.title());
        }
        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw new InvalidOperationException("description must not be blank");
            }
            donation.setDescription(request.description());
        }
        if (request.category() != null) {
            donation.setCategory(request.category());
        }
        if (request.quantity() != null) {
            donation.setQuantity(request.quantity());
        }
        if (request.quantityUnit() != null) {
            donation.setQuantityUnit(request.quantityUnit());
        }
        if (request.condition() != null) {
            donation.setCondition(request.condition());
        }
        if (request.city() != null) {
            if (request.city().isBlank()) {
                throw new InvalidOperationException("city must not be blank");
            }
            donation.setCity(request.city());
        }
        if (request.locality() != null) {
            donation.setLocality(request.locality());
        }
        if (request.pincode() != null) {
            donation.setPincode(request.pincode());
        }
    }
}
