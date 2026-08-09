package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;

public record UserResponse(
        long id,
        String name,
        String email,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
