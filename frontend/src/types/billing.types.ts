/**
 * Billing domain types (billing-service).
 */

export type PlanType = 'FREE' | 'PLUS' | 'PRO' | 'PREMIUM';

export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export type OrderStatus = 'CREATED' | 'PAID' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

/** A storage plan definition. */
export interface Plan {
  planType: PlanType;
  /** Storage quota in bytes. */
  storageBytes: number;
  /** Monthly price in INR. */
  priceInr: number;
  currency: string;
  billingPeriod: string;
  features: string[];
}

/** The authenticated user's current subscription. */
export interface Subscription {
  userId: number;
  planType: PlanType;
  status: SubscriptionStatus;
  startsAt: string | null;
  expiresAt: string | null;
  /** Storage quota granted by the subscribed plan, in bytes. */
  quotaBytes: number;
}

/** A payment order created for a plan upgrade. */
export interface PaymentOrder {
  orderUuid: string;
  planType: PlanType;
  amountInr: number;
  currency: string;
  status: OrderStatus;
  /** Provider (Razorpay) order id — present only when the provider is configured. */
  providerOrderId: string | null;
  /** Provider public key id — client-safe, required by the checkout SDK. */
  providerKeyId: string | null;
  createdAt: string | null;
  paidAt: string | null;
  /** True when the payment provider is configured and checkout can start. */
  checkoutAvailable: boolean;
}

/** The user's storage quota. */
export interface Quota {
  planType: PlanType;
  quotaBytes: number;
}

/** Request body for creating a payment order. */
export interface CreateOrderRequest {
  planType: PlanType;
}

/** Client confirmation payload after a Razorpay checkout. */
export interface VerifyPaymentRequest {
  orderUuid: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}
