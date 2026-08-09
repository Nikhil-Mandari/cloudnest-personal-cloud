import { useEffect, useState, type ClipboardEvent, type KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import { KeyRound, Mail, ShieldCheck, Unlock } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { APP_ROUTES } from '@/constants/routes';
import {
  PASSWORD_MIN_LENGTH,
  PASSWORD_PATTERN,
  PASSWORD_REQUIREMENTS_MESSAGE,
} from '@/constants/validation';
import { authService } from '@/services/auth.service';
import type { OtpDispatchResponse, ResetTokenResponse } from '@/types';
import { cn } from '@/utils/cn';
import { getErrorMessage } from '@/utils/error';

const OTP_LENGTH = 6;

type Step = 'email' | 'otp' | 'password' | 'done';

export function ForgotPasswordPage() {
  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [digits, setDigits] = useState<string[]>(() => Array(OTP_LENGTH).fill(''));
  const [challengeToken, setChallengeToken] = useState<string | null>(null);
  const [resetToken, setResetToken] = useState<string | null>(null);
  const [devOtp, setDevOtp] = useState<string | undefined>(undefined);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resendAfter, setResendAfter] = useState<number>(60);
  const [resendKey, setResendKey] = useState(0);

  const requestOtp = useMutation({
    mutationFn: (address: string) => authService.forgotPassword({ email: address }),
    onSuccess: ({ data }: { data: { data: OtpDispatchResponse } }) => {
      const result = data.data;
      if (!result.sent || !result.challengeToken) {
        toast.success('If an account exists for that email, a reset code has been sent.');
        setStep('done');
        return;
      }
      setChallengeToken(result.challengeToken);
      setDevOtp(result.devOtp ?? undefined);
      setResendAfter(result.resendAfterSeconds ?? 60);
      setStep('otp');
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Something went wrong. Please try again.')),
  });

  const verifyOtp = useMutation({
    mutationFn: (code: string) =>
      authService.verifyForgotPassword({ challengeToken: challengeToken ?? undefined, code }),
    onSuccess: ({ data }: { data: { data: ResetTokenResponse } }) => {
      setResetToken(data.data.resetToken);
      setStep('password');
    },
    onError: (error) => toast.error(getErrorMessage(error, 'That code didn\'t work. Please try again.')),
  });

  const resetPassword = useMutation({
    mutationFn: (password: string) =>
      authService.resetPassword({ resetToken: resetToken ?? '', newPassword: password }),
    onSuccess: () => {
      toast.success('Password reset — sign in with your new password.');
      setStep('done');
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Could not reset the password. Please try again.')),
  });

  const resendOtp = useMutation({
    mutationFn: () => authService.resendOtp({ email, challengeToken: challengeToken ?? undefined }),
    onSuccess: ({ data }) => {
      toast.success('A new code has been sent.');
      setDevOtp(data.data.devOtp ?? undefined);
      setChallengeToken(data.data.challengeToken ?? challengeToken);
      setResendAfter(data.data.resendAfterSeconds ?? 60);
      setResendKey((key) => key + 1);
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Could not resend the code.')),
  });

  // Countdown for the resend button — reset during render (React-recommended)
  // whenever the resend key or cooldown changes.
  const [remaining, setRemaining] = useState(resendAfter);
  const [prevResend, setPrevResend] = useState<{ key: number; after: number }>({
    key: resendKey,
    after: resendAfter,
  });
  if (prevResend.key !== resendKey || prevResend.after !== resendAfter) {
    setPrevResend({ key: resendKey, after: resendAfter });
    setRemaining(resendAfter);
  }
  useEffect(() => {
    if (remaining <= 0) {
      return;
    }
    const timer = window.setInterval(() => setRemaining((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [remaining]);

  const code = digits.join('');
  const complete = code.length === OTP_LENGTH;
  const passwordsMatch = newPassword === confirmPassword;
  const passwordValid = PASSWORD_PATTERN.test(newPassword);

  const handleDigitChange = (index: number, value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(-1);
    setDigits((prev) => {
      const next = [...prev];
      next[index] = cleaned;
      return next;
    });
  };

  const handlePaste = (event: ClipboardEvent) => {
    event.preventDefault();
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (!pasted) {
      return;
    }
    setDigits(Array.from({ length: OTP_LENGTH }, (_, i) => pasted[i] ?? ''));
  };

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      document.getElementById(`otp-${index - 1}`)?.focus();
    }
  };

  return (
    <div>
      <div className="mb-8 text-center">
        <div className="bg-brand-600/10 dark:bg-brand-500/15 mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl">
          {step === 'email' ? (
            <Mail className="text-brand-600 h-7 w-7 dark:text-brand-400" />
          ) : step === 'otp' ? (
            <KeyRound className="text-brand-600 h-7 w-7 dark:text-brand-400" />
          ) : (
            <Unlock className="text-brand-600 h-7 w-7 dark:text-brand-400" />
          )}
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
          {step === 'email' && 'Reset your password'}
          {step === 'otp' && 'Check your email'}
          {step === 'password' && 'Choose a new password'}
          {step === 'done' && 'All set'}
        </h1>
        <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400">
          {step === 'email' && 'Enter your account email and we\'ll send you a reset code.'}
          {step === 'otp' && `We sent a code to ${email}.`}
          {step === 'password' && 'Pick a strong password you haven\'t used before.'}
          {step === 'done' && 'Your password has been updated.'}
        </p>
      </div>

      {step === 'email' && (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (!email) {
              return;
            }
            requestOtp.mutate(email);
          }}
          noValidate
          className="space-y-5"
        >
          <Input
            label="Email"
            type="email"
            placeholder="you@example.com"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            leftIcon={<Mail className="h-4 w-4" />}
          />
          <Button type="submit" size="lg" fullWidth isLoading={requestOtp.isPending}>
            Send reset code
          </Button>
        </form>
      )}

      {step === 'otp' && (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (complete) {
              verifyOtp.mutate(code);
            }
          }}
          noValidate
        >
          {devOtp && (
            <div className="mb-6 flex items-center gap-3 rounded-xl border border-amber-300/60 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300">
              <ShieldCheck className="h-5 w-5 shrink-0" />
              <div>
                <p className="font-semibold">Development mode</p>
                <p>
                  Your code:{' '}
                  <span className="font-mono text-base font-bold tracking-widest">{devOtp}</span>
                </p>
              </div>
            </div>
          )}

          <div className="flex justify-center gap-2 sm:gap-2.5" onPaste={handlePaste}>
            {digits.map((digit, index) => (
              <input
                key={index}
                id={`otp-${index}`}
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

          <Button type="submit" size="lg" fullWidth isLoading={verifyOtp.isPending} disabled={!complete} className="mt-7">
            Verify code
          </Button>

          <div className="mt-5 text-center text-sm text-gray-500 dark:text-gray-400">
            {remaining > 0 ? (
              <p>
                Resend available in{' '}
                <span className="font-mono font-semibold text-gray-700 dark:text-gray-200">
                  {remaining}s
                </span>
              </p>
            ) : (
              <button
                type="button"
                onClick={() => resendOtp.mutate()}
                disabled={resendOtp.isPending}
                className="text-brand-600 hover:text-brand-700 dark:text-brand-400 font-medium transition-colors hover:underline disabled:opacity-60"
              >
                Resend code
              </button>
            )}
          </div>
        </form>
      )}

      {step === 'password' && (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (passwordValid && passwordsMatch) {
              resetPassword.mutate(newPassword);
            }
          }}
          noValidate
          className="space-y-5"
        >
          <PasswordInput
            label="New password"
            placeholder="Enter a new password"
            autoComplete="new-password"
            hint={PASSWORD_REQUIREMENTS_MESSAGE}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            error={
              newPassword && !passwordValid
                ? PASSWORD_REQUIREMENTS_MESSAGE
                : undefined
            }
          />
          <PasswordInput
            label="Confirm new password"
            placeholder="Repeat the new password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            error={
              confirmPassword && !passwordsMatch
                ? 'Passwords do not match'
                : undefined
            }
          />
          <Button
            type="submit"
            size="lg"
            fullWidth
            isLoading={resetPassword.isPending}
            disabled={!passwordValid || !passwordsMatch || newPassword.length < PASSWORD_MIN_LENGTH}
          >
            Reset password
          </Button>
        </form>
      )}

      {step === 'done' && (
        <div className="space-y-5">
          <div className="flex items-center justify-center">
            <div className="bg-emerald-500/10 grid h-14 w-14 place-items-center rounded-2xl">
              <ShieldCheck className="text-emerald-500 h-7 w-7" />
            </div>
          </div>
          <Button size="lg" fullWidth onClick={() => (window.location.href = APP_ROUTES.login)}>
            Go to sign in
          </Button>
        </div>
      )}

      {step !== 'done' && (
        <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
          Remembered it?{' '}
          <Link
            to={APP_ROUTES.login}
            className="text-brand-600 hover:text-brand-700 dark:text-brand-400 font-medium transition-colors hover:underline"
          >
            Back to sign in
          </Link>
        </p>
      )}
    </div>
  );
}
