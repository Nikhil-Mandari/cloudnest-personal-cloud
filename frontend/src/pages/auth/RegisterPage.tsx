import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Mail, User } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { APP_ROUTES } from '@/constants/routes';
import {
  EMAIL_PATTERN,
  NAME_MIN_LENGTH,
  PASSWORD_MIN_LENGTH,
  PASSWORD_PATTERN,
  PASSWORD_REQUIREMENTS_MESSAGE,
} from '@/constants/validation';
import { useAuthMutations } from '@/hooks/useAuthMutations';
import type { RegisterFormValues } from '@/types';

const registerDefaultValues: RegisterFormValues = {
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
};

export function RegisterPage() {
  const { registerMutation } = useAuthMutations();

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    defaultValues: registerDefaultValues,
  });

  const onSubmit = (values: RegisterFormValues) => {
    registerMutation.mutate(values);
  };

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
          Create your account
        </h1>
        <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400">
          Start organising your personal cloud in minutes.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        <Input
          label="Full name"
          type="text"
          placeholder="Nikhil Mandari"
          autoComplete="name"
          leftIcon={<User className="h-4 w-4" />}
          error={errors.fullName?.message}
          {...register('fullName', {
            required: 'Full name is required',
            minLength: {
              value: NAME_MIN_LENGTH,
              message: `Name must be at least ${NAME_MIN_LENGTH} characters`,
            },
          })}
        />

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
          placeholder="Create a password"
          autoComplete="new-password"
          hint={PASSWORD_REQUIREMENTS_MESSAGE}
          error={errors.password?.message}
          {...register('password', {
            required: 'Password is required',
            minLength: {
              value: PASSWORD_MIN_LENGTH,
              message: `Password must be at least ${PASSWORD_MIN_LENGTH} characters`,
            },
            pattern: { value: PASSWORD_PATTERN, message: PASSWORD_REQUIREMENTS_MESSAGE },
          })}
        />

        <PasswordInput
          label="Confirm password"
          placeholder="Repeat your password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword', {
            required: 'Please confirm your password',
            validate: (value) => value === getValues('password') || 'Passwords do not match',
          })}
        />

        <Button type="submit" size="lg" fullWidth isLoading={registerMutation.isPending}>
          Create account
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
        Already have an account?{' '}
        <Link
          to={APP_ROUTES.login}
          className="text-brand-600 hover:text-brand-700 dark:text-brand-400 font-medium transition-colors hover:underline"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
}
