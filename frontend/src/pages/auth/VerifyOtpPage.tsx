import { useEffect, useRef, useState, type ClipboardEvent, type KeyboardEvent } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { KeyRound, RefreshCw, ShieldCheck } from 'lucide-react';
import { toast } from 'react-toastify';

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
  const [devOtp, setDevOtp] = useState<string | undefined>(state?.devOtp);
  const [resendKey, setResendKey] = useState(0);
  const remaining = useCountdown(state?.resendAfterSeconds ?? DEFAULT_COOLDOWN, resendKey);

  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);

  const email = state?.email ?? 'your email address';
  const isLogin = state?.purpose === 'login';
  const code = digits.join('');
  const complete = code.length === OTP_LENGTH;

  // No flow context (direct navigation) — send the user back to sign in.
  if (!state) {
    return <Navigate to={APP_ROUTES.login} replace />;
  }

  const handleDigitChange = (index: number, value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(-1);
    setDigits((prev) => {
      const next = [...prev];
      next[index] = cleaned;
      return next;
    });
    if (cleaned && index < OTP_LENGTH - 1) {
      inputsRef.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
  };

  const handlePaste = (event: ClipboardEvent) => {
    event.preventDefault();
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (!pasted) {
      return;
    }
    const next = Array(OTP_LENGTH).fill('');
    for (let i = 0; i < pasted.length; i++) {
      next[i] = pasted[i];
    }
    setDigits(next);
    inputsRef.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus();
  };

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
        onSuccess: ({ data }) => {
          toast.success('A new code has been sent.');
          setDevOtp(data.data.devOtp ?? undefined);
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
          {isLogin ? 'Enter your sign-in code' : 'Verify your email'}
        </h1>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
          We sent a {OTP_LENGTH}-digit code to <span className="font-medium text-gray-700 dark:text-gray-200">{email}</span>.
          <br />
          It expires in <span className="font-medium">{expiryMinutes} minutes</span>.
        </p>
      </div>

      {devOtp && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-amber-300/60 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300">
          <ShieldCheck className="h-5 w-5 shrink-0" />
          <div>
            <p className="font-semibold">Development mode</p>
            <p>
              Email delivery is disabled, so here is your code:{' '}
              <span className="font-mono text-base font-bold tracking-widest">{devOtp}</span>
            </p>
          </div>
        </div>
      )}

      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        noValidate
      >
        <div className="flex justify-center gap-2 sm:gap-2.5" onPaste={handlePaste}>
          {digits.map((digit, index) => (
            <input
              key={index}
              ref={(el) => {
                inputsRef.current[index] = el;
              }}
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              aria-label={`Digit ${index + 1}`}
              value={digit}
              onChange={(event) => handleDigitChange(index, event.target.value)}
              onKeyDown={(event) => handleKeyDown(index, event)}
              className={cn(
                'h-14 w-11 rounded-xl border text-center font-mono text-xl font-bold shadow-sm transition-all sm:h-16 sm:w-13',
                'focus:border-brand-500 focus:ring-brand-500/25 focus:outline-none focus:ring-2',
                digit
                  ? 'border-brand-500 bg-brand-50 text-brand-700 dark:border-brand-500 dark:bg-brand-500/10 dark:text-brand-300'
                  : 'border-gray-300 bg-white text-gray-900 dark:border-gray-700 dark:bg-gray-950 dark:text-white',
              )}
            />
          ))}
        </div>

        <Button
          type="submit"
          size="lg"
          fullWidth
          isLoading={verifyOtpMutation.isPending}
          disabled={!complete}
          className="mt-7"
        >
          {isLogin ? 'Verify & sign in' : 'Activate account'}
        </Button>
      </form>

      <div className="mt-5 text-center text-sm text-gray-500 dark:text-gray-400">
        {remaining > 0 ? (
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
        {isLogin ? (
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
