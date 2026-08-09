package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {

    Optional<Donation> findByIdAndDonorId(Long id, Long donorId);

    Page<Donation> findAllByDonorId(Long donorId, Pageable pageable);

    Page<Donation> findAllByStatus(DonationStatus status, Pageable pageable);

    Page<Donation> findAllByStatusAndCategory(DonationStatus status, Category category, Pageable pageable);

    Page<Donation> findAllByStatusAndCategoryAndCity(DonationStatus status, Category category, String city, Pageable pageable);

    List<Donation> findAllByStatusAndCategoryAndQuantityUnitAndCityIgnoreCase(
            DonationStatus status, Category category, QuantityUnit quantityUnit, String city);
}
