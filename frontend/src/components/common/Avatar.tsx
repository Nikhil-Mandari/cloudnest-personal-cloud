import { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';

import { useAvatar } from '@/hooks/useAvatar';
import { cn } from '@/utils/cn';
import { getInitials } from '@/utils/format';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

const sizeClasses: Record<AvatarSize, { wrapper: string; text: string }> = {
  xs: { wrapper: 'h-6 w-6', text: 'text-[9px]' },
  sm: { wrapper: 'h-8 w-8', text: 'text-[11px]' },
  md: { wrapper: 'h-10 w-10', text: 'text-sm' },
  lg: { wrapper: 'h-16 w-16', text: 'text-xl' },
  xl: { wrapper: 'h-24 w-24', text: 'text-3xl' },
};

export interface AvatarProps {
  /** Display name used for the initials fallback (and alt text). */
  name: string;
  /** `User.avatarUrl` — a numeric file id (uploaded avatar) or a plain URL. */
  avatarUrl?: string | null;
  size?: AvatarSize;
  className?: string;
}

/**
 * User avatar: renders the uploaded image when one exists (resolved through
 * the file-service via `useAvatar`), otherwise a branded initials circle.
 * Falls back to initials if the image cannot be fetched or fails to render.
 */
export function Avatar({ name, avatarUrl, size = 'md', className }: AvatarProps) {
  const url = useAvatar(avatarUrl);
  const [failed, setFailed] = useState(false);
  const initials = getInitials(name);
  const classes = sizeClasses[size];
  const showImage = Boolean(url) && !failed;

  return (
    <span
      className={cn(
        'relative grid shrink-0 place-items-center overflow-hidden rounded-full',
        classes.wrapper,
        !showImage && 'from-brand-500 to-accent-600 bg-linear-to-br text-white',
        !showImage && size === 'xl' && 'shadow-lg shadow-brand-500/25',
        className,
      )}
      aria-label={name}
      role="img"
      title={name}
    >
      <AnimatePresence initial={false} mode="popLayout">
        {showImage ? (
          <motion.img
            key={url ?? 'avatar'}
            src={url ?? undefined}
            alt={name}
            loading="lazy"
            decoding="async"
            initial={{ opacity: 0, scale: 0.92 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="absolute inset-0 h-full w-full object-cover"
            onError={() => setFailed(true)}
          />
        ) : (
          <motion.span
            key="initials"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className={cn('font-bold tracking-wide select-none', classes.text)}
          >
            {initials}
          </motion.span>
        )}
      </AnimatePresence>
    </span>
  );
}
