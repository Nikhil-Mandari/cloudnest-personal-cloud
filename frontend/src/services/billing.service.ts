import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  ApiResponse,
  CreateOrderRequest,
  PaymentOrder,
  Plan,
  Quota,
  Subscription,
  VerifyPaymentRequest,
} from '@/types';

/**
 * Billing service (billing-service microservice).
 *
 * Payment safety: quota is only upgraded after the server verifies the
 * Razorpay signature — a frontend callback alone never grants storage.
 */
export const billingService = {
  getPlans: () => apiClient.get<ApiResponse<Plan[]>>(API_ENDPOINTS.billing.plans),

  getSubscription: () =>
    apiClient.get<ApiResponse<Subscription>>(API_ENDPOINTS.billing.subscription),

  getQuota: () => apiClient.get<ApiResponse<Quota>>(API_ENDPOINTS.billing.quota),

  getOrderHistory: () =>
    apiClient.get<ApiResponse<PaymentOrder[]>>(API_ENDPOINTS.billing.orders),

  /** Creates a payment order (503 when the payment provider is not configured). */
  createOrder: (body: CreateOrderRequest) =>
    apiClient.post<ApiResponse<PaymentOrder>>(API_ENDPOINTS.billing.createOrder, body, {
      silent: true,
      // A 401 here is a gateway/session edge case, never the payment result —
      // keep the payment UI in control of its own error state.
      skipAuthRedirect: true,
    }),

  /** Confirms a payment after checkout — verified server-side by signature. */
  verifyPayment: (body: VerifyPaymentRequest) =>
    apiClient.post<ApiResponse<PaymentOrder>>(API_ENDPOINTS.billing.verifyOrder, body, {
      silent: true,
      skipAuthRedirect: true,
    }),

  /** Cancels a pending order (user abandoned the checkout). */
  cancelOrder: (orderUuid: string) =>
    apiClient.post<ApiResponse<PaymentOrder>>(API_ENDPOINTS.billing.cancelOrder(orderUuid)),
};
