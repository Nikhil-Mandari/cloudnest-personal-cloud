import type { ReactNode } from 'react';

export interface HighlightProps {
  text: string;
  /** The raw search term; matching is case-insensitive. */
  query?: string;
  className?: string;
}

/** Splits `text` into segments and highlights every case-insensitive match. */
export function Highlight({ text, query, className }: HighlightProps) {
  const term = query?.trim();
  if (!term) {
    return <>{text}</>;
  }

  const lower = text.toLowerCase();
  const needle = term.toLowerCase();
  const parts: ReactNode[] = [];
  let cursor = 0;

  for (;;) {
    const index = lower.indexOf(needle, cursor);
    if (index === -1) {
      parts.push(text.slice(cursor));
      break;
    }
    if (index > cursor) {
      parts.push(text.slice(cursor, index));
    }
    parts.push(
      <mark
        key={`${index}-${needle}`}
        className="bg-brand-500/20 text-brand-900 rounded-[3px] px-0 py-0 font-semibold dark:bg-brand-400/25 dark:text-brand-100"
      >
        {text.slice(index, index + needle.length)}
      </mark>,
    );
    cursor = index + needle.length;
  }

  return <span className={className}>{parts}</span>;
}
