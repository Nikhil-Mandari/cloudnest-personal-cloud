import { cn } from '@/utils/cn';

export interface HighlightProps {
  text: string;
  query?: string;
  className?: string;
}

export function Highlight({ text, query, className }: HighlightProps) {
  if (!query) {
    return <span className={className}>{text}</span>;
  }

  const index = text.toLowerCase().indexOf(query.toLowerCase());
  if (index === -1) {
    return <span className={className}>{text}</span>;
  }

  const before = text.slice(0, index);
  const match = text.slice(index, index + query.length);
  const after = text.slice(index + query.length);

  return (
    <span className={className}>
      {before}
      <mark className={cn('rounded bg-amber-300/40 px-0.5 text-inherit dark:bg-amber-400/30')}>
        {match}
      </mark>
      {after}
    </span>
  );
}