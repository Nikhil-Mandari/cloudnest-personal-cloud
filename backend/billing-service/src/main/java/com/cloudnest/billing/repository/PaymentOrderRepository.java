package com.cloudnest.billing.repository;

import com.cloudnest.billing.entity.OrderStatus;
import com.cloudnest.billing.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderUuid(String orderUuid);

    Optional<PaymentOrder> findByProviderOrderId(String providerOrderId);

    Optional<PaymentOrder> findByProviderPaymentId(String providerPaymentId);

    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PaymentOrder> findByStatus(OrderStatus status);
}
