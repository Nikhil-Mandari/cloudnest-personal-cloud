package com.cloudnest.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A storage plan definition (FREE / PLUS / PRO / PREMIUM).
 * <p>
 * Plans are seeded at startup and are read-only from the API. The quota
 * (in bytes) is the single source of truth for the File Service upload
 * enforcement.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, unique = true, length = 20)
    private PlanType planType;

    /** Storage quota granted by this plan, in bytes. */
    @Column(name = "storage_bytes", nullable = false)
    private Long storageBytes;

    /** Monthly price in INR (0 for the FREE plan). */
    @Column(name = "price_inr", nullable = false)
    private Long priceInr;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "billing_period", nullable = false, length = 16)
    private String billingPeriod;

    /**
     * JSON array of human-readable feature bullets, e.g.
     * {@code ["100 GB storage", "1 GB max file size"]}.
     */
    @Column(name = "features", length = 1000)
    private String features;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
