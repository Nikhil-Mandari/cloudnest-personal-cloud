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
 * Quotas: FREE 5 GB, PLUS 100 GB, PRO 500 GB, PREMIUM 1 TB.
 * Prices (INR, monthly) are product defaults defined here and mirrored by
 * the frontend plan metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        Map<PlanType, Plan> plans = new LinkedHashMap<>();
        plans.put(PlanType.FREE, plan(PlanType.FREE, 5L * 1024 * 1024 * 1024, 0L,
                "[\"5 GB storage\", \"100 MB max file size\", \"File sharing & links\", \"Community support\"]"));
        plans.put(PlanType.PLUS, plan(PlanType.PLUS, 100L * 1024 * 1024 * 1024, 199L,
                "[\"100 GB storage\", \"500 MB max file size\", \"File sharing & links\", \"Email support\"]"));
        plans.put(PlanType.PRO, plan(PlanType.PRO, 500L * 1024 * 1024 * 1024, 499L,
                "[\"500 GB storage\", \"1 GB max file size\", \"Priority file processing\", \"Priority support\"]"));
        plans.put(PlanType.PREMIUM, plan(PlanType.PREMIUM, 1024L * 1024 * 1024 * 1024, 999L,
                "[\"1 TB storage\", \"5 GB max file size\", \"Priority file processing\", \"Dedicated support\"]"));

        int created = 0;
        for (Map.Entry<PlanType, Plan> entry : plans.entrySet()) {
            if (planRepository.findByPlanType(entry.getKey()).isEmpty()) {
                planRepository.save(entry.getValue());
                created++;
            }
        }
        if (created > 0) {
            log.info("Seeded {} billing plan(s)", created);
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
