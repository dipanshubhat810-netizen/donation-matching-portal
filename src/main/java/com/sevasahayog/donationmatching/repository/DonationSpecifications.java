package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import org.springframework.data.jpa.domain.Specification;

public final class DonationSpecifications {

    private DonationSpecifications() {
    }

    public static Specification<Donation> statusIs(DonationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Donation> categoryIs(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Donation> cityEquals(String city) {
        String normalized = city.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), normalized);
    }

    public static Specification<Donation> textMatches(String text) {
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like));
    }
}
