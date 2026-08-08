import { Fragment, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

import { cn } from '@/utils/cn';

export interface BreadcrumbItem {
  label: string;
  href?: string;
  icon?: ReactNode;
  /** When provided, the crumb renders as a button instead of a link. */
  onClick?: () => void;
}

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  className?: string;
}

export function Breadcrumb({ items, className }: BreadcrumbProps) {
  return (
    <nav aria-label="Breadcrumb" className={cn('flex items-center gap-1.5 text-sm', className)}>
      {items.map((item, index) => {
        const isLast = index === items.length - 1;

        return (
          <Fragment key={`${item.label}-${index}`}>
            {index > 0 && <ChevronRight className="h-3.5 w-3.5 shrink-0 text-gray-400" />}
            {isLast ? (
              <span
                aria-current="page"
                className="flex items-center gap-1.5 font-medium text-gray-900 dark:text-white"
              >
                {item.icon}
                {item.label}
              </span>
            ) : item.onClick ? (
              <button
                type="button"
                onClick={item.onClick}
                className="flex cursor-pointer items-center gap-1.5 text-gray-500 transition-colors hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
              >
                {item.icon}
                {item.label}
              </button>
            ) : item.href ? (
              <Link
                to={item.href}
                className="flex items-center gap-1.5 text-gray-500 transition-colors hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
              >
                {item.icon}
                {item.label}
              </Link>
            ) : (
              <span className="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
                {item.icon}
                {item.label}
              </span>
            )}
          </Fragment>
        );
      })}
    </nav>
  );
}
