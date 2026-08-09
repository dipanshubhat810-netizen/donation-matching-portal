package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.User;

public record UserSummaryResponse(
        long id,
        String name
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getName());
    }
}
