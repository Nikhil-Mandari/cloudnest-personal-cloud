package com.cloudnest.billing.repository;

import com.cloudnest.billing.entity.Plan;
import com.cloudnest.billing.entity.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByPlanType(PlanType planType);

    List<Plan> findByActiveTrueOrderByStorageBytesAsc();
}
