package com.sevasahayog.donationmatching.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        @NotBlank(message = "password must not be blank")
        String password
) {
}
