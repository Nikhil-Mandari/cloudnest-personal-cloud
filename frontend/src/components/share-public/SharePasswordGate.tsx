import { useState } from 'react';
import { Eye, EyeOff, Lock, ShieldCheck } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

export interface SharePasswordGateProps {
  /** Display name of the shared resource (used in the helper text). */
  resourceName?: string | null;
  /** True while the verify request is in flight. */
  submitting: boolean;
  /** Server-side error (wrong password, etc.). */
  error?: string;
  onSubmit: (password: string) => void;
  /** Clears the parent's error as the user starts typing again. */
  onInputChange?: () => void;
}

/** Password gate shown before a protected public share can be viewed. */
export function SharePasswordGate({
  resourceName,
  submitting,
  error,
  onSubmit,
  onInputChange,
}: SharePasswordGateProps) {
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="space-y-5">
      <div className="flex flex-col items-center text-center">
        <div className="bg-amber-500/10 text-amber-600 dark:bg-amber-500/10 dark:text-amber-400 mb-4 grid h-14 w-14 place-items-center rounded-2xl">
          <Lock className="h-7 w-7" />
        </div>
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Password required</h2>
        <p className="mt-1.5 text-sm text-gray-500 dark:text-gray-400">
          {resourceName ? (
            <>
              “{resourceName}” is password protected. Enter the share password to view or download
              it.
            </>
          ) : (
            'This shared item is password protected. Enter the share password to continue.'
          )}
        </p>
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit(password);
        }}
        className="space-y-3"
      >
        <Input
          label="Share password"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={(event) => {
            setPassword(event.target.value);
            onInputChange?.();
          }}
          placeholder="Enter the share password"
          autoFocus
          error={error}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowPassword((visible) => !visible)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
        />

        <Button
          type="submit"
          fullWidth
          isLoading={submitting}
          leftIcon={<ShieldCheck className="h-4 w-4" />}
        >
          Unlock
        </Button>
      </form>
    </div>
  );
}
