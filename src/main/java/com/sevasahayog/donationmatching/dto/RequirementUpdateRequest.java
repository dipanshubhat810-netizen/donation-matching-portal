package com.sevasahayog.donationmatching.dto;

import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Urgency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RequirementUpdateRequest(
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        Category category,

        @Positive(message = "quantity must be greater than 0")
        @Digits(integer = 9, fraction = 3, message = "quantity must have at most 9 integer digits and 3 decimal places")
        BigDecimal quantity,

        QuantityUnit quantityUnit,

        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @Size(max = 100, message = "locality must be at most 100 characters")
        String locality,

        @Size(max = 20, message = "pincode must be at most 20 characters")
        String pincode,

        Urgency urgency
) {
}
