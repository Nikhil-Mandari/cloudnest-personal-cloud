import { useCallback, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { AnimatePresence, motion } from 'framer-motion';
import { Check, CreditCard, Crown, HardDrive, Loader2, Lock, ShieldCheck, Sparkles } from 'lucide-react';
import { toast } from 'react-toastify';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Modal } from '@/components/ui/Modal';
import { useStorageOverviewQuery } from '@/hooks/useStorageAnalytics';
import {
  useCreateOrderMutation,
  useOrderHistoryQuery,
  usePlansQuery,
  useSubscriptionQuery,
  useVerifyPaymentMutation,
} from '@/hooks/useBilling';
import { billingService } from '@/services/billing.service';
import type { PaymentOrder, Plan, PlanType } from '@/types';
import { cn } from '@/utils/cn';
import { formatBytes } from '@/utils/format';

/**
 * Razorpay Checkout SDK global (loaded lazily from checkout.razorpay.com).
 * The SDK renders the provider's official payment UI — including the
 * dynamic per-order UPI QR — from the order id + public key id.
 */
interface RazorpayInstance {
  open: () => void;
}
interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (response: { razorpay_payment_id: string; razorpay_signature: string }) => void;
  modal?: { ondismiss: () => void };
  theme?: { color: string };
}
declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => RazorpayInstance;
  }
}

const PLAN_META: Record<PlanType, { tagline: string; accent: string; icon: typeof Crown }> = {
  FREE: { tagline: 'Start your personal cloud', accent: 'from-gray-500 to-gray-600', icon: HardDrive },
  PLUS: { tagline: 'For everyday storage', accent: 'from-sky-500 to-blue-600', icon: Sparkles },
  PRO: { tagline: 'For power users & creators', accent: 'from-violet-500 to-indigo-600', icon: Crown },
  PREMIUM: { tagline: 'Maximum storage & priority', accent: 'from-amber-500 to-orange-600', icon: Crown },
};

type CheckoutState =
  | { kind: 'idle' }
  | { kind: 'confirm'; plan: Plan }
  | { kind: 'creating'; plan: Plan }
  | { kind: 'opening' }
  | { kind: 'verifying' }
  | { kind: 'success'; order: PaymentOrder }
  | { kind: 'blocked'; message: string };

/** Loads the Razorpay Checkout SDK script once. */
function loadRazorpaySdk(): Promise<boolean> {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }
    const existing = document.getElementById('razorpay-checkout-sdk');
    if (existing) {
      existing.addEventListener('load', () => resolve(Boolean(window.Razorpay)), { once: true });
      existing.addEventListener('error', () => resolve(false), { once: true });
      return;
    }
    const script = document.createElement('script');
    script.id = 'razorpay-checkout-sdk';
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => resolve(Boolean(window.Razorpay));
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

/** A single plan card with its features. */
function PlanCard({
  plan,
  isCurrent,
  onUpgrade,
  busy,
}: {
  plan: Plan;
  isCurrent: boolean;
  onUpgrade: (plan: Plan) => void;
  busy: boolean;
}) {
  const meta = PLAN_META[plan.planType];
  const Icon = meta.icon;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
      whileHover={{ y: -4 }}
      className={cn(
        'relative flex flex-col rounded-2xl border p-6 transition-colors',
        isCurrent
          ? 'border-brand-500/60 bg-brand-500/[0.04] shadow-lg shadow-brand-500/10 dark:bg-brand-400/[0.04]'
          : 'border-gray-200 bg-white hover:border-brand-300 dark:border-gray-800 dark:bg-gray-900 dark:hover:border-brand-700',
      )}
    >
      {isCurrent && (
        <span className="bg-brand-600 text-white absolute -top-3 left-6 rounded-full px-3 py-0.5 text-xs font-semibold shadow-sm">
          Current plan
        </span>
      )}

      <div
        className={cn(
          'mb-4 grid h-12 w-12 place-items-center rounded-xl bg-gradient-to-br text-white shadow-md',
          meta.accent,
        )}
      >
        <Icon className="h-6 w-6" />
      </div>

      <h3 className="text-lg font-bold text-gray-900 dark:text-white">
        {plan.planType.charAt(0) + plan.planType.slice(1).toLowerCase()}
      </h3>
      <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{meta.tagline}</p>

      <div className="mt-4 flex items-baseline gap-1">
        <span className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white">
          {plan.priceInr === 0 ? 'Free' : `₹${plan.priceInr}`}
        </span>
        {plan.priceInr > 0 && (
          <span className="text-sm text-gray-400 dark:text-gray-500">/ month</span>
        )}
      </div>
      <p className="mt-1 text-sm font-medium text-brand-600 dark:text-brand-400">
        {formatBytes(plan.storageBytes)} storage
      </p>

      <ul className="mt-5 flex-1 space-y-2.5">
        {plan.features.map((feature) => (
          <li key={feature} className="flex items-start gap-2 text-sm text-gray-600 dark:text-gray-300">
            <Check className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
            <span>{feature}</span>
          </li>
        ))}
      </ul>

      <Button
        className="mt-6 w-full"
        variant={isCurrent ? 'outline' : 'primary'}
        disabled={isCurrent || busy}
        onClick={() => onUpgrade(plan)}
      >
        {isCurrent ? 'Active plan' : 'Upgrade'}
      </Button>
    </motion.div>
  );
}

/** Storage & plan management page (Phase 7–9). */
export function StoragePlansPage() {
  const { data: plans } = usePlansQuery();
  const { data: subscription } = useSubscriptionQuery();
  const { data: overview } = useStorageOverviewQuery();
  const { data: orders } = useOrderHistoryQuery();

  const createOrder = useCreateOrderMutation();
  const verifyPayment = useVerifyPaymentMutation();

  const [checkout, setCheckout] = useState<CheckoutState>({ kind: 'idle' });
  const checkoutRef = useRef(checkout);
  checkoutRef.current = checkout;

  const used = overview?.storageUsed ?? 0;
  const quota = subscription?.quotaBytes ?? 30 * 1024 ** 3;
  const remaining = Math.max(0, quota - used);
  const percent = quota > 0 ? Math.min(100, (used / quota) * 100) : 0;

  const startUpgrade = useCallback(
    (plan: Plan) => {
      if (plan.priceInr === 0) {
        toast.info('You are already on the FREE plan');
        return;
      }
      setCheckout({ kind: 'confirm', plan });
    },
    [],
  );

  const closeCheckout = useCallback(() => {
    setCheckout({ kind: 'idle' });
  }, []);

  /** Creates the order, then opens the provider checkout (or shows blocked). */
  const handleConfirm = useCallback(async () => {
    const current = checkoutRef.current;
    if (current.kind !== 'confirm') {
      return;
    }
    setCheckout({ kind: 'creating', plan: current.plan });
    try {
      const { data } = await createOrder.mutateAsync({ planType: current.plan.planType });
      const order = data.data;
      if (!order.providerOrderId) {
        // Defensive: the provider must have issued a real order id to open checkout.
        setCheckout({
          kind: 'blocked',
          message: 'The payment provider could not create a payment session. Please try again later.',
        });
        return;
      }
      await openRazorpayCheckout(order);
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 503) {
        setCheckout({
          kind: 'blocked',
          message: 'The payment provider is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET to enable payments.',
        });
      } else {
        setCheckout({ kind: 'idle' });
        toast.error('Could not create the payment order. Please try again.');
      }
    }
  }, [createOrder]);

  /**
   * Opens the Razorpay Checkout SDK — the official payment UI with the
   * dynamic per-order UPI QR. On success the signature is verified
   * server-side before the plan is upgraded.
   */
  const openRazorpayCheckout = useCallback(
    async (order: PaymentOrder) => {
      setCheckout({ kind: 'opening' });
      const loaded = await loadRazorpaySdk();
      if (!loaded || !window.Razorpay) {
        setCheckout({ kind: 'idle' });
        toast.error('The payment gateway could not be loaded. Please try again.');
        return;
      }

      try {
        const instance = new window.Razorpay({
          key: order.providerKeyId ?? '',
          amount: order.amountInr * 100,
          currency: order.currency ?? 'INR',
          name: 'CloudNest',
          description: `${order.planType} plan upgrade`,
          order_id: order.providerOrderId ?? '',
          handler: async (response) => {
            setCheckout({ kind: 'verifying' });
            try {
              await verifyPayment.mutateAsync({
                orderUuid: order.orderUuid,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              });
              setCheckout({ kind: 'success', order });
              toast.success(`${order.planType} plan activated 🎉`);
            } catch {
              setCheckout({ kind: 'idle' });
              toast.error('Payment verification failed. Your plan was not changed.');
            }
          },
          modal: {
            // The user dismissed the secure checkout window — cancel the order.
            ondismiss: () => {
              void billingService.cancelOrder(order.orderUuid).catch(() => undefined);
              setCheckout((prev) => (prev.kind === 'opening' ? { kind: 'idle' } : prev));
            },
          },
          theme: { color: '#7c3aed' },
        });
        instance.open();
      } catch {
        setCheckout({ kind: 'idle' });
        toast.error('Could not start the payment session.');
      }
    },
    [verifyPayment],
  );

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Storage plans</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Choose a plan that fits your storage needs. Payments are processed securely and your
          quota is upgraded only after the payment is verified.
        </p>
      </div>

      {/* Usage meter */}
      <Card className="mb-10 overflow-hidden">
        <div className="bg-gradient-to-r from-brand-600 to-indigo-600 px-6 py-5 text-white">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Crown className="h-6 w-6" />
              <div>
                <p className="text-sm text-white/70">Current plan</p>
                <p className="text-lg font-bold">
                  {subscription?.planType ?? 'FREE'}
                  {subscription?.expiresAt
                    ? ` · renews ${new Date(subscription.expiresAt).toLocaleDateString()}`
                    : ''}
                </p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-sm text-white/70">Storage used</p>
              <p className="text-lg font-bold">
                {formatBytes(used)} <span className="text-sm font-medium text-white/70">of {formatBytes(quota)}</span>
              </p>
            </div>
          </div>
          <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/20">
            <motion.div
              className="h-full rounded-full bg-white"
              initial={{ width: 0 }}
              animate={{ width: `${percent}%` }}
              transition={{ duration: 0.6, ease: 'easeOut' }}
            />
          </div>
          <p className="mt-2 text-xs text-white/70">
            {formatBytes(remaining)} remaining · {percent.toFixed(1)}% used
          </p>
        </div>
      </Card>

      {/* Plan cards */}
      {plans && plans.length > 0 ? (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
          {plans.map((plan) => (
            <PlanCard
              key={plan.planType}
              plan={plan}
              isCurrent={subscription?.planType === plan.planType}
              busy={createOrder.isPending}
              onUpgrade={startUpgrade}
            />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-5 xl:grid-cols-4">
          {(['FREE', 'PLUS', 'PRO', 'PREMIUM'] as PlanType[]).map((type) => (
            <div
              key={type}
              className="h-64 animate-pulse rounded-2xl border border-gray-200 bg-gray-100 dark:border-gray-800 dark:bg-gray-800/60"
            />
          ))}
        </div>
      )}

      {/* Order history */}
      {orders && orders.length > 0 && (
        <Card className="mt-10">
          <div className="px-6 py-4">
            <h3 className="text-base font-semibold text-gray-900 dark:text-white">Payment history</h3>
          </div>
          <ul className="divide-y divide-gray-100 dark:divide-gray-800">
            {orders.slice(0, 6).map((order) => (
              <li key={order.orderUuid} className="flex items-center justify-between px-6 py-3.5 text-sm">
                <div>
                  <p className="font-medium text-gray-800 dark:text-gray-200">
                    {order.planType} plan · ₹{order.amountInr}
                  </p>
                  <p className="text-xs text-gray-400">
                    {order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}
                  </p>
                </div>
                <span
                  className={cn(
                    'rounded-full px-2.5 py-0.5 text-xs font-medium',
                    order.status === 'PAID' && 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
                    order.status === 'CREATED' && 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
                    order.status === 'FAILED' && 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
                    (order.status === 'CANCELLED' || order.status === 'EXPIRED') &&
                      'bg-gray-500/10 text-gray-500 dark:text-gray-400',
                  )}
                >
                  {order.status}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {/* Checkout modal */}
      <AnimatePresence>
        {checkout.kind !== 'idle' && (
          <CheckoutModal
            state={checkout}
            onClose={closeCheckout}
            onConfirm={handleConfirm}
            busy={createOrder.isPending || verifyPayment.isPending}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

function CheckoutModal({
  state,
  onClose,
  onConfirm,
  busy,
}: {
  state: Exclude<CheckoutState, { kind: 'idle' }>;
  onClose: () => void;
  onConfirm: () => void;
  busy: boolean;
}) {
  const plan = state.kind === 'confirm' || state.kind === 'creating' ? state.plan : undefined;
  const title =
    state.kind === 'success'
      ? 'Payment confirmed'
      : state.kind === 'blocked'
        ? 'Payments unavailable'
        : `Upgrade to ${plan?.planType ?? ''}`;

  return (
    <Modal open onClose={onClose} title={title} description="Secure checkout" hideCloseButton>
      {state.kind === 'blocked' ? (
        <div className="flex flex-col items-center py-4 text-center">
          <div className="bg-amber-500/10 text-amber-600 dark:text-amber-400 mb-4 grid h-14 w-14 place-items-center rounded-2xl">
            <Lock className="h-7 w-7" />
          </div>
          <p className="max-w-sm text-sm text-gray-600 dark:text-gray-300">{state.message}</p>
          <p className="mt-3 max-w-sm text-xs text-gray-400">
            Set <code className="rounded bg-gray-100 px-1 py-0.5 dark:bg-gray-800">RAZORPAY_KEY_ID</code> and{' '}
            <code className="rounded bg-gray-100 px-1 py-0.5 dark:bg-gray-800">RAZORPAY_KEY_SECRET</code> environment
            variables, then restart the billing service. No payment is charged and no quota is changed.
          </p>
          <Button className="mt-5" variant="outline" onClick={onClose}>
            Close
          </Button>
        </div>
      ) : state.kind === 'success' ? (
        <div className="flex flex-col items-center py-4 text-center">
          <motion.div
            initial={{ scale: 0.6, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: 'spring', stiffness: 260, damping: 18 }}
            className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 mb-4 grid h-14 w-14 place-items-center rounded-2xl"
          >
            <ShieldCheck className="h-7 w-7" />
          </motion.div>
          <p className="text-sm font-medium text-gray-800 dark:text-gray-200">
            Your {state.order.planType} plan is now active.
          </p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Your storage quota has been upgraded. Refresh the page to see the new limit.
          </p>
          <Button className="mt-5" onClick={onClose}>
            Done
          </Button>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex items-center justify-between rounded-xl bg-gray-50 px-4 py-3 dark:bg-gray-800/60">
            <div className="flex items-center gap-3">
              <CreditCard className="h-5 w-5 text-brand-600 dark:text-brand-400" />
              <div>
                <p className="text-sm font-medium text-gray-800 dark:text-gray-200">
                  {plan?.planType} plan
                </p>
                <p className="text-xs text-gray-400">Monthly subscription</p>
              </div>
            </div>
            <p className="text-lg font-bold text-gray-900 dark:text-white">₹{plan?.priceInr ?? 0}</p>
          </div>

          <p className="text-center text-xs text-gray-400 dark:text-gray-500">
            You will be redirected to the secure Razorpay checkout with UPI / card payment — including a
            dynamic payment QR. Your quota is upgraded only after the payment is verified by the server.
          </p>

          <div className="flex items-center justify-center gap-3">
            <Button variant="outline" onClick={onClose} disabled={busy}>
              Cancel
            </Button>
            <Button onClick={onConfirm} isLoading={busy}>
              {busy ? 'Processing' : 'Continue to payment'}
            </Button>
          </div>

          {(state.kind === 'creating' || state.kind === 'opening' || state.kind === 'verifying') && (
            <div className="flex items-center justify-center gap-2 text-sm text-gray-500 dark:text-gray-400">
              <Loader2 className="h-4 w-4 animate-spin" />
              {state.kind === 'verifying'
                ? 'Verifying your payment…'
                : state.kind === 'opening'
                  ? 'Payment window opened — complete the payment in the secure Razorpay window…'
                  : 'Contacting the payment gateway…'}
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
