import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

import { Input, type InputProps } from './Input';

export type PasswordInputProps = Omit<InputProps, 'type' | 'rightIcon'>;

export function PasswordInput(props: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <Input
      {...props}
      type={visible ? 'text' : 'password'}
      rightIcon={
        <button
          type="button"
          aria-label={visible ? 'Hide password' : 'Show password'}
          onClick={() => setVisible((value) => !value)}
          className="text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-200"
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      }
    />
  );
}
