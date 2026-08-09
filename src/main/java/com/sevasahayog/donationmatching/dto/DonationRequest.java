package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DonationRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotBlank(message = "description must not be blank")
        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "category must not be null")
        Category category,

        @NotNull(message = "quantity must not be null")
        @Positive(message = "quantity must be greater than 0")
        @Digits(integer = 9, fraction = 3, message = "quantity must have at most 9 integer digits and 3 decimal places")
        BigDecimal quantity,

        @NotNull(message = "quantityUnit must not be null")
        QuantityUnit quantityUnit,

        @NotNull(message = "condition must not be null")
        Condition condition,

        @NotBlank(message = "city must not be blank")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @Size(max = 100, message = "locality must be at most 100 characters")
        String locality,

        @Size(max = 20, message = "pincode must be at most 20 characters")
        String pincode
) {
}
