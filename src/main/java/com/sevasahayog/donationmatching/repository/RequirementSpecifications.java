package com.sevasahayog.donationmatching.repository;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import org.springframework.data.jpa.domain.Specification;

public final class RequirementSpecifications {

    private RequirementSpecifications() {
    }

    public static Specification<Requirement> statusIs(RequirementStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Requirement> categoryIs(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Requirement> cityEquals(String city) {
        String normalized = city.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), normalized);
    }

    public static Specification<Requirement> textMatches(String text) {
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like));
    }
}
