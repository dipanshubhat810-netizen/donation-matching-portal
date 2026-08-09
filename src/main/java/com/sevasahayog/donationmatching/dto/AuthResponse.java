package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Role;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        Role role
) {
}
