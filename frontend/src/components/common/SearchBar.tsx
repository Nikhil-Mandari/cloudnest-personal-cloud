import { useEffect, useState } from 'react';
import { Search, X } from 'lucide-react';

import { useDebounce } from '@/hooks/useDebounce';
import { cn } from '@/utils/cn';

export interface SearchBarProps {
  placeholder?: string;
  defaultValue?: string;
  /** Debounce delay in ms before `onSearch` fires. */
  debounceMs?: number;
  onSearch?: (value: string) => void;
  className?: string;
}

export function SearchBar({
  placeholder = 'Search…',
  defaultValue = '',
  debounceMs = 300,
  onSearch,
  className,
}: SearchBarProps) {
  const [value, setValue] = useState(defaultValue);
  const debouncedValue = useDebounce(value, debounceMs);

  useEffect(() => {
    onSearch?.(debouncedValue);
  }, [debouncedValue, onSearch]);

  return (
    <div className={cn('relative', className)}>
      <Search className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
      <input
        type="search"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="focus:border-brand-500 focus:ring-brand-500/25 h-10 w-full rounded-lg border border-gray-300 bg-white pr-9 pl-9 text-sm text-gray-900 shadow-sm transition-colors placeholder:text-gray-400 focus:ring-2 focus:outline-none dark:border-gray-700 dark:bg-gray-950 dark:text-white dark:placeholder:text-gray-500"
      />
      {value && (
        <button
          type="button"
          onClick={() => setValue('')}
          aria-label="Clear search"
          className="absolute top-1/2 right-2.5 -translate-y-1/2 rounded p-0.5 text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-200"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
