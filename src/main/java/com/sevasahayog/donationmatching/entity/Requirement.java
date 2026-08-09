package com.sevasahayog.donationmatching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "requirements", indexes = {
        @Index(name = "idx_requirements_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_requirements_status", columnList = "status"),
        @Index(name = "idx_requirements_category", columnList = "category"),
        @Index(name = "idx_requirements_city", columnList = "city")
})
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @Check(name = "ck_requirements_quantity_positive", constraints = "quantity_required > 0")
    @Column(name = "quantity_required", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false, length = 50)
    private QuantityUnit quantityUnit;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String locality;

    @Column(length = 20)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Urgency urgency;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RequirementStatus status = RequirementStatus.SUBMITTED;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
