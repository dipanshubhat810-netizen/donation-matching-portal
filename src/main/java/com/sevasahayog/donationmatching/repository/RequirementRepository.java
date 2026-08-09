package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, Long>, JpaSpecificationExecutor<Requirement> {

    Optional<Requirement> findByIdAndReceiverId(Long id, Long receiverId);

    Page<Requirement> findAllByReceiverId(Long receiverId, Pageable pageable);

    Page<Requirement> findAllByStatus(RequirementStatus status, Pageable pageable);

    Page<Requirement> findAllByStatusAndCategory(RequirementStatus status, Category category, Pageable pageable);

    Page<Requirement> findAllByStatusAndCategoryAndCity(RequirementStatus status, Category category, String city, Pageable pageable);
}
