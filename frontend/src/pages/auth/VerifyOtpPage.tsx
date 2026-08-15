import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { KeyRound, RefreshCw } from 'lucide-react';
import { toast } from 'react-toastify';

import { OtpInput } from '@/components/auth/OtpInput';
import { Button } from '@/components/ui/Button';
import { APP_ROUTES } from '@/constants/routes';
import { useAuthMutations } from '@/hooks/useAuthMutations';
import type { VerifyOtpState } from '@/types';
import { cn } from '@/utils/cn';
import { getErrorMessage } from '@/utils/error';

const OTP_LENGTH = 6;
const DEFAULT_COOLDOWN = 60;

/** Seconds until the resend button unlocks (drives the countdown timer). */
function useCountdown(initialSeconds: number | undefined, resetKey: number) {
  const [remaining, setRemaining] = useState(() => initialSeconds ?? DEFAULT_COOLDOWN);

  // Reset the countdown whenever the resend key or cooldown changes — adjust
  // state during render (React-recommended pattern) instead of in an effect.
  const current = initialSeconds ?? DEFAULT_COOLDOWN;
  const [prevReset, setPrevReset] = useState<{ key: number; seconds: number }>({
    key: resetKey,
    seconds: current,
  });
  if (prevReset.key !== resetKey || prevReset.seconds !== current) {
    setPrevReset({ key: resetKey, seconds: current });
    setRemaining(current);
  }

  useEffect(() => {
    if (remaining <= 0) {
      return;
    }
    const timer = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [remaining]);

  return remaining;
}

function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return m > 0 ? `${m}:${String(s).padStart(2, '0')}` : `${s}s`;
}

export function VerifyOtpPage() {
  const location = useLocation();
  const state = (location.state as VerifyOtpState | null) ?? null;

  const { verifyOtpMutation, resendOtpMutation } = useAuthMutations();

  const [digits, setDigits] = useState<string[]>(() => Array(OTP_LENGTH).fill(''));
  const [resendKey, setResendKey] = useState(0);
  const remaining = useCountdown(state?.resendAfterSeconds ?? DEFAULT_COOLDOWN, resendKey);

  const submitRef = useRef<HTMLButtonElement | null>(null);

  const email = state?.email ?? 'your email address';
  const isLogin = state?.purpose === 'login';
  const isTwoFactor = state?.purpose === '2fa';
  const code = digits.join('');
  const complete = code.length === OTP_LENGTH;

  // No flow context (direct navigation) — send the user back to sign in.
  if (!state) {
    return <Navigate to={APP_ROUTES.login} replace />;
  }

  const submit = () => {
    if (!complete) {
      return;
    }
    verifyOtpMutation.mutate({
      purpose: state.purpose,
      code,
      email: state.email,
      challengeToken: state.challengeToken,
      rememberDevice: state.rememberDevice,
      from: state.from,
    });
  };

  const resend = () => {
    resendOtpMutation.mutate(
      { email: state.email, challengeToken: state.challengeToken },
      {
        onSuccess: () => {
          toast.success('A new code has been sent.');
          setResendKey((key) => key + 1);
        },
        onError: (error) => {
          toast.error(getErrorMessage(error, 'Could not resend the code. Please try again.'));
        },
      },
    );
  };

  const expiryMinutes = state?.otpExpiryMinutes ?? 5;

  return (
    <div>
      <div className="mb-8 text-center">
        <div className="bg-brand-600/10 dark:bg-brand-500/15 mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl">
          <KeyRound className="text-brand-600 h-7 w-7 dark:text-brand-400" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
          {isTwoFactor ? 'Enter your authenticator code' : isLogin ? 'Enter your sign-in code' : 'Verify your email'}
        </h1>
        {isTwoFactor ? (
          <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
            Open your authenticator app for <span className="font-medium text-gray-700 dark:text-gray-200">{email}</span>
            <br />
            and enter the {OTP_LENGTH}-digit code (or a backup code).
          </p>
        ) : (
          <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
            We sent a {OTP_LENGTH}-digit code to <span className="font-medium text-gray-700 dark:text-gray-200">{email}</span>.
            <br />
            It expires in <span className="font-medium">{expiryMinutes} minutes</span>.
          </p>
        )}
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        noValidate
      >
        <OtpInput digits={digits} onChange={setDigits} submitButtonRef={submitRef} />

        <Button
          ref={submitRef}
          type="submit"
          size="lg"
          fullWidth
          isLoading={verifyOtpMutation.isPending}
          disabled={!complete}
          className="mt-7"
        >
          {isLogin || isTwoFactor ? 'Verify & sign in' : 'Activate account'}
        </Button>
      </form>

      <div className="mt-5 text-center text-sm text-gray-500 dark:text-gray-400">
        {isTwoFactor ? (
          <p>
            Can&apos;t find your code? Use one of your{' '}
            <span className="font-medium text-gray-700 dark:text-gray-200">backup codes</span> instead.
          </p>
        ) : remaining > 0 ? (
          <p>
            Didn&apos;t get the code? Resend available in{' '}
            <span className="font-mono font-semibold text-gray-700 dark:text-gray-200">
              {formatCountdown(remaining)}
            </span>
          </p>
        ) : (
          <button
            type="button"
            onClick={resend}
            disabled={resendOtpMutation.isPending}
            className="text-brand-600 hover:text-brand-700 dark:text-brand-400 inline-flex items-center gap-1.5 font-medium transition-colors hover:underline disabled:opacity-60"
          >
            <RefreshCw className={cn('h-4 w-4', resendOtpMutation.isPending && 'animate-spin')} />
            Resend code
          </button>
        )}
      </div>

      <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
        {isLogin || isTwoFactor ? (
          <>
            Wrong device?{' '}
            <Link
              to={APP_ROUTES.login}
              className="text-brand-600 hover:text-brand-700 dark:text-brand-400 font-medium transition-colors hover:underline"
            >
              Sign in again
            </Link>
          </>
        ) : (
          <>
            Already verified?{' '}
            <Link
              to={APP_ROUTES.login}
              className="text-brand-600 hover:text-brand-700 dark:text-brand-400 font-medium transition-colors hover:underline"
            >
              Sign in
            </Link>
          </>
        )}
      </p>
    </div>
  );
}
