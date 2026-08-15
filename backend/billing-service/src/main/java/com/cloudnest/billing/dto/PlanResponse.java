package com.cloudnest.billing.dto;

import com.cloudnest.billing.entity.Plan;
import com.cloudnest.billing.entity.PlanType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Public representation of a storage plan.
 */
@Getter
@Builder
@AllArgsConstructor
public class PlanResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PlanType planType;

    /** Storage quota in bytes. */
    private final Long storageBytes;

    /** Monthly price in INR. */
    private final Long priceInr;

    private final String currency;

    private final String billingPeriod;

    private final List<String> features;

    public static PlanResponse from(Plan plan) {
        return PlanResponse.builder()
                .planType(plan.getPlanType())
                .storageBytes(plan.getStorageBytes())
                .priceInr(plan.getPriceInr())
                .currency(plan.getCurrency())
                .billingPeriod(plan.getBillingPeriod())
                .features(parseFeatures(plan.getFeatures()))
                .build();
    }

    private static List<String> parseFeatures(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return MAPPER.readValue(trimmed, new TypeReference<List<String>>() {
                });
            } catch (Exception ignored) {
                // fall through to the comma-split below
            }
        }
        return java.util.Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
