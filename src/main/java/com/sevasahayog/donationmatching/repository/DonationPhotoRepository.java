package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.DonationPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationPhotoRepository extends JpaRepository<DonationPhoto, Long> {

    List<DonationPhoto> findAllByDonationId(Long donationId);
}
