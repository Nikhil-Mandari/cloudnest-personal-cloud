import { useForm } from 'react-hook-form';
import { Link, useLocation } from 'react-router-dom';
import { Fingerprint, KeyRound, Mail } from 'lucide-react';

import { SocialLoginButtons } from '@/components/auth/SocialLoginButtons';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { APP_ROUTES } from '@/constants/routes';
import { EMAIL_PATTERN } from '@/constants/validation';
import { isPasskeySupported, useAuthMutations } from '@/hooks/useAuthMutations';
import type { LoginFormValues } from '@/types';

export function LoginPage() {
  const location = useLocation();
  const { loginMutation, passkeyLoginMutation } = useAuthMutations();

  const passkeysSupported = isPasskeySupported();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    defaultValues: { email: '', password: '', rememberMe: false },
  });

  // Redirect back to the page the user originally tried to visit.
  const from = (location.state as { from?: string } | null)?.from;

  const onSubmit = (values: LoginFormValues) => {
    loginMutation.mutate({ ...values, from });
  };

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
          Welcome back
        </h1>
        <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400">
          Sign in to continue to your cloud.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        <Input
          label="Email"
          type="email"
          placeholder="you@example.com"
          autoComplete="email"
          leftIcon={<Mail className="h-4 w-4" />}
          error={errors.email?.message}
          {...register('email', {
            required: 'Email is required',
            pattern: { value: EMAIL_PATTERN, message: 'Enter a valid email address' },
          })}
        />

        <PasswordInput
          label="Password"
          placeholder="Enter your password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password', { required: 'Password is required' })}
        />

        <div className="flex items-center justify-between">
          <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-600 dark:text-gray-300">
            <input
              type="checkbox"
              className="text-brand-600 focus:ring-brand-500 h-4 w-4 rounded border-gray-300 dark:border-gray-600 dark:bg-gray-900"
              {...register('rememberMe')}
            />
            <span>Remember this device</span>
          </label>
          <Link
            to={APP_ROUTES.forgotPassword}
            className="text-brand-600 hover:text-brand-700 dark:text-brand-400 text-sm font-medium transition-colors hover:underline"
          >
            Forgot password?
          </Link>
        </div>

        <p className="flex items-center gap-1.5 text-xs text-gray-400 dark:text-gray-500">
          <KeyRound className="h-3.5 w-3.5" />
          For extra security, we email a one-time code for every new sign-in.
        </p>

        <Button type="submit" size="lg" fullWidth isLoading={loginMutation.isPending}>
          Sign in
        </Button>
      </form>

      <SocialLoginButtons />

      {passkeysSupported ? (
        <>
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200 dark:border-gray-800" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-white px-3 text-xs text-gray-400 dark:bg-gray-950">or</span>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            size="lg"
            fullWidth
            leftIcon={<Fingerprint className="h-4 w-4" />}
            isLoading={passkeyLoginMutation.isPending}
            onClick={() => passkeyLoginMutation.mutate(from ? { from } : {})}
          >
            Sign in with a passkey
          </Button>
          <p className="mt-2 text-center text-xs text-gray-400 dark:text-gray-500">
            Use Face ID, Touch ID, Windows Hello or a security key.
          </p>
        </>
      ) : (
        <p className="mt-4 flex items-center justify-center gap-1.5 text-xs text-gray-400 dark:text-gray-500">
          <Fingerprint className="h-3.5 w-3.5" />
          Passkeys are not supported by this browser.
        </p>
      )}

      <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
        Don&apos;t have an account?{' '}
        <Link
          to={APP_ROUTES.register}
          className="text-brand-600 hover:text-brand-700 dark:text-brand-400 font-medium transition-colors hover:underline"
        >
          Create one
        </Link>
      </p>
    </div>
  );
}
