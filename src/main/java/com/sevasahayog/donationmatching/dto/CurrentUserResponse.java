package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Role;

public record CurrentUserResponse(
        Long id,
        String email,
        Role role
) {
}
