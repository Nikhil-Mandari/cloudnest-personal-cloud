package com.cloudnest.billing.config;

import com.cloudnest.billing.entity.Plan;
import com.cloudnest.billing.entity.PlanType;
import com.cloudnest.billing.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds the four storage plans on startup (idempotent).
 * <p>
 * Quotas: FREE 30 GB, PLUS 100 GB, PRO 500 GB, PREMIUM 1 TB.
 * Prices (INR, monthly) are product defaults defined here and mirrored by
 * the frontend plan metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanSeeder implements CommandLineRunner {

    /** Free-tier storage quota (30 GB) — the default for every account. */
    static final long FREE_STORAGE_BYTES = 30L * 1024 * 1024 * 1024;

    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        Map<PlanType, Plan> plans = new LinkedHashMap<>();
        plans.put(PlanType.FREE, plan(PlanType.FREE, FREE_STORAGE_BYTES, 0L,
                "[\"30 GB storage\", \"100 MB max file size\", \"File sharing & links\", \"Community support\"]"));
        plans.put(PlanType.PLUS, plan(PlanType.PLUS, 100L * 1024 * 1024 * 1024, 199L,
                "[\"100 GB storage\", \"500 MB max file size\", \"File sharing & links\", \"Email support\"]"));
        plans.put(PlanType.PRO, plan(PlanType.PRO, 500L * 1024 * 1024 * 1024, 499L,
                "[\"500 GB storage\", \"1 GB max file size\", \"Priority file processing\", \"Priority support\"]"));
        plans.put(PlanType.PREMIUM, plan(PlanType.PREMIUM, 1024L * 1024 * 1024 * 1024, 999L,
                "[\"1 TB storage\", \"5 GB max file size\", \"Priority file processing\", \"Dedicated support\"]"));

        int created = 0;
        int upgraded = 0;
        for (Map.Entry<PlanType, Plan> entry : plans.entrySet()) {
            Plan desired = entry.getValue();
            Plan existing = planRepository.findByPlanType(entry.getKey()).orElse(null);
            if (existing == null) {
                planRepository.save(desired);
                created++;
            } else if (existing.getPlanType() == PlanType.FREE
                    && existing.getStorageBytes() != desired.getStorageBytes()) {
                // Existing installs seeded the old 5 GB FREE quota — raise the
                // free tier to 30 GB without touching any paid plan.
                existing.setStorageBytes(desired.getStorageBytes());
                existing.setFeatures(desired.getFeatures());
                planRepository.save(existing);
                upgraded++;
            }
        }
        if (created > 0) {
            log.info("Seeded {} billing plan(s)", created);
        }
        if (upgraded > 0) {
            log.info("Raised the FREE plan storage quota to 30 GB for {} existing plan record(s)", upgraded);
        }
    }

    private Plan plan(PlanType type, long storageBytes, long priceInr, String features) {
        return Plan.builder()
                .planType(type)
                .storageBytes(storageBytes)
                .priceInr(priceInr)
                .currency("INR")
                .billingPeriod("MONTHLY")
                .features(features)
                .active(true)
                .build();
    }
}
