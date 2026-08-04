import { useEffect, useRef, useState, type ReactNode } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Check, ChevronDown } from 'lucide-react';

import { useClickOutside } from '@/hooks/useClickOutside';
import { cn } from '@/utils/cn';

export interface DropdownOption<T extends string> {
  value: T;
  label: string;
  description?: string;
  icon?: ReactNode;
  disabled?: boolean;
}

export interface DropdownMenuProps<T extends string> {
  value: T;
  options: ReadonlyArray<DropdownOption<T>>;
  onChange: (value: T) => void;
  /** Accessible name for the trigger button. */
  label: string;
  /** Optional content shown in the trigger (defaults to the selected label). */
  triggerContent?: ReactNode;
  icon?: ReactNode;
  align?: 'left' | 'right';
  className?: string;
  /** Renders a trailing adornment for each row (e.g. a count badge). */
  optionMeta?: (value: T) => ReactNode;
}

/** Generic trigger + panel dropdown with outside-click and Escape handling. */
export function DropdownMenu<T extends string>({
  value,
  options,
  onChange,
  label,
  triggerContent,
  icon,
  align = 'right',
  className,
  optionMeta,
}: DropdownMenuProps<T>) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  useClickOutside(containerRef, () => setOpen(false));

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open]);

  const selected = options.find((option) => option.value === value);

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <button
        type="button"
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
        className={cn(
          'flex h-10 items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 text-sm font-medium text-gray-700 shadow-sm transition-colors',
          'focus-visible:ring-brand-500/50 hover:bg-gray-50 focus-visible:ring-2 focus-visible:outline-none',
          'dark:border-gray-700 dark:bg-gray-900 dark:text-gray-200 dark:hover:bg-gray-800/70',
          open && 'border-brand-500 ring-brand-500/25 dark:border-brand-500 ring-2',
        )}
      >
        {icon && <span className="text-gray-400">{icon}</span>}
        <span className="max-w-28 truncate">{triggerContent ?? selected?.label}</span>
        <ChevronDown
          className={cn(
            'h-4 w-4 text-gray-400 transition-transform duration-200',
            open && 'rotate-180',
          )}
        />
      </button>

      <AnimatePresence>
        {open && (
          <motion.ul
            role="listbox"
            aria-label={label}
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.14, ease: 'easeOut' }}
            className={cn(
              'absolute z-40 mt-2 w-56 overflow-hidden rounded-xl border border-gray-200 bg-white py-1.5 shadow-xl shadow-gray-900/10 dark:border-gray-700 dark:bg-gray-900 dark:shadow-black/40',
              align === 'right' ? 'right-0' : 'left-0',
            )}
          >
            {options.map((option) => {
              const isSelected = option.value === value;
              return (
                <li key={option.value} role="option" aria-selected={isSelected}>
                  <button
                    type="button"
                    disabled={option.disabled}
                    onClick={() => {
                      onChange(option.value);
                      setOpen(false);
                    }}
                    className={cn(
                      'flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors',
                      isSelected
                        ? 'bg-brand-500/10 text-brand-700 dark:text-brand-300'
                        : 'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-800/70',
                      option.disabled && 'pointer-events-none opacity-50',
                    )}
                  >
                    {option.icon && (
                      <span className="grid h-4 w-4 shrink-0 place-items-center text-gray-400">
                        {option.icon}
                      </span>
                    )}
                    <span className="flex-1 truncate">
                      {option.label}
                      {option.description && (
                        <span className="block truncate text-xs text-gray-400 dark:text-gray-500">
                          {option.description}
                        </span>
                      )}
                    </span>
                    {optionMeta?.(option.value)}
                    {isSelected && <Check className="h-4 w-4 shrink-0" />}
                  </button>
                </li>
              );
            })}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  );
}
