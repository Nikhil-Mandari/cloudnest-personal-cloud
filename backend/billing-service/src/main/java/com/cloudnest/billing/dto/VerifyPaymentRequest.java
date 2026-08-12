package com.cloudnest.billing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Client confirmation payload after a Razorpay checkout.
 * <p>
 * The signature is verified server-side against the provider secret before
 * the order is marked PAID — a frontend callback alone never grants quota.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyPaymentRequest {

    @NotBlank(message = "Order UUID must not be blank")
    private String orderUuid;

    @NotBlank(message = "Razorpay payment id must not be blank")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature must not be blank")
    private String razorpaySignature;
}
