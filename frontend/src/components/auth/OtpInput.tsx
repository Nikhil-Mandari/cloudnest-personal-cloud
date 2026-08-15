import { useRef, type ClipboardEvent, type KeyboardEvent, type RefObject } from 'react';

import { cn } from '@/utils/cn';

interface OtpInputProps {
  /** Current digits, one string per box (empty string = empty box). */
  digits: string[];
  /** Called with the next digits array whenever any box changes. */
  onChange: (next: string[]) => void;
  /**
   * Optional ref to the form's submit button. Once all digits are entered the
   * submit button receives focus so the user can confirm the code with Enter.
   */
  submitButtonRef?: RefObject<HTMLButtonElement | null>;
}

/**
 * Shared 6-box OTP input used by the signup verification and forgot-password
 * flows. Typing advances to the next box automatically, Backspace moves to the
 * previous box, pasting a full code fills every box, and non-numeric input is
 * rejected. Only the digits state is lifted to the caller — focus management
 * and input behavior live here.
 */
export function OtpInput({ digits, onChange, submitButtonRef }: OtpInputProps) {
  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);
  const length = digits.length;

  const handleDigitChange = (index: number, value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = cleaned;
    onChange(next);
    if (cleaned && index < length - 1) {
      inputsRef.current[index + 1]?.focus();
    } else if (cleaned && index === length - 1 && next.every(Boolean)) {
      // All boxes are full — hand focus to the confirm button. Automatic
      // submission is deliberately not part of this UX; the user confirms.
      submitButtonRef?.current?.focus();
    }
  };

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
  };

  const handlePaste = (event: ClipboardEvent<HTMLDivElement>) => {
    event.preventDefault();
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, length);
    if (!pasted) {
      return;
    }
    const next = Array(length).fill('');
    for (let i = 0; i < pasted.length; i++) {
      next[i] = pasted[i];
    }
    onChange(next);
    inputsRef.current[Math.min(pasted.length, length - 1)]?.focus();
  };

  return (
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
          maxLength={1}
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
  );
}
