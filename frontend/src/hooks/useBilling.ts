import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { billingService } from '@/services/billing.service';
import type { CreateOrderRequest, VerifyPaymentRequest } from '@/types';

/** Query keys for the billing domain. */
export const BILLING_QUERY_KEYS = {
  plans: ['billing', 'plans'] as const,
  subscription: ['billing', 'subscription'] as const,
  quota: ['billing', 'quota'] as const,
  orders: ['billing', 'orders'] as const,
};

/** Fetches the available storage plans. */
export function usePlansQuery() {
  return useQuery({
    queryKey: BILLING_QUERY_KEYS.plans,
    queryFn: async () => {
      const { data } = await billingService.getPlans();
      return data.data;
    },
    staleTime: 5 * 60 * 1000,
  });
}

/** Fetches the authenticated user's current subscription. */
export function useSubscriptionQuery() {
  return useQuery({
    queryKey: BILLING_QUERY_KEYS.subscription,
    queryFn: async () => {
      const { data } = await billingService.getSubscription();
      return data.data;
    },
  });
}

/** Fetches the authenticated user's storage quota. */
export function useQuotaQuery() {
  return useQuery({
    queryKey: BILLING_QUERY_KEYS.quota,
    queryFn: async () => {
      const { data } = await billingService.getQuota();
      return data.data;
    },
  });
}

/** Fetches the authenticated user's payment order history. */
export function useOrderHistoryQuery() {
  return useQuery({
    queryKey: BILLING_QUERY_KEYS.orders,
    queryFn: async () => {
      const { data } = await billingService.getOrderHistory();
      return data.data;
    },
  });
}

/**
 * Creates a payment order. The returned order carries the provider order id
 * and {@code checkoutAvailable} — only then can checkout start.
 */
export function useCreateOrderMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateOrderRequest) => billingService.createOrder(body),
    onSuccess: () => {
      // New order → the history list may have changed.
      void queryClient.invalidateQueries({ queryKey: BILLING_QUERY_KEYS.orders });
    },
  });
}

/**
 * Verifies a payment after checkout. On success the server marks the order
 * PAID and upgrades the subscription — refresh quota + subscription + history.
 */
export function useVerifyPaymentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: VerifyPaymentRequest) => billingService.verifyPayment(body),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: BILLING_QUERY_KEYS.subscription }),
        queryClient.invalidateQueries({ queryKey: BILLING_QUERY_KEYS.quota }),
        queryClient.invalidateQueries({ queryKey: BILLING_QUERY_KEYS.orders }),
      ]);
    },
  });
}
