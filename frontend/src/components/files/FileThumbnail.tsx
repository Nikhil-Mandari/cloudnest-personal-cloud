import { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';

import { useFileThumbnail } from '@/hooks/useFileThumbnail';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { FileIcon } from './FileIcon';

export interface FileThumbnailProps {
  file: FileItem;
  /** Size of the rendered tile (controls icon fallback too). */
  size?: 'sm' | 'md' | 'lg';
  /** Container classes — override the default tile size (e.g. `w-full aspect-[4/3]`). */
  className?: string;
  /** Extra classes merged onto the rendered `<img>` (hover zoom etc.). */
  imgClassName?: string;
}

/** Tracks whether an element has scrolled into (or near) the viewport. */
function useInView<T extends HTMLElement>(rootMargin = '240px') {
  const ref = useRef<T>(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element || typeof IntersectionObserver === 'undefined') {
      setInView(true);
      return;
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          observer.disconnect();
        }
      },
      { rootMargin },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [rootMargin]);

  return { ref, inView };
}

const tileClasses = {
  sm: 'h-8 w-8 rounded-lg',
  md: 'h-12 w-12 rounded-xl',
  lg: 'h-20 w-20 rounded-2xl',
};

/**
 * Real file thumbnail (image / video first frame / PDF page one), lazily
 * generated only when the tile is about to enter the viewport. Falls back to
 * the branded `FileIcon` for unsupported types or while loading fails.
 */
export function FileThumbnail({
  file,
  size = 'md',
  className,
  imgClassName,
}: FileThumbnailProps) {
  const { ref, inView } = useInView<HTMLSpanElement>();
  const { state } = useFileThumbnail(file, inView);

  return (
    <span
      ref={ref}
      aria-hidden="true"
      className={cn(
        'relative grid shrink-0 place-items-center overflow-hidden',
        tileClasses[size],
        className,
      )}
    >
      {state.status === 'ready' && state.url ? (
        <motion.img
          key={state.url}
          src={state.url}
          alt=""
          loading="lazy"
          decoding="async"
          initial={{ opacity: 0, scale: 1.04 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.25, ease: 'easeOut' }}
          className={cn('absolute inset-0 h-full w-full object-cover', imgClassName)}
        />
      ) : inView && state.status === 'idle' ? (
        <span className="absolute inset-0 animate-pulse bg-gray-100 dark:bg-gray-800" />
      ) : (
        <FileIcon file={file} size={size} showExtension />
      )}

      {/* Soft inner border so thumbnails read as tiles, not cut-off photos */}
      <span className="pointer-events-none absolute inset-0 rounded-[inherit] ring-1 ring-gray-900/5 ring-inset dark:ring-white/10" />
    </span>
  );
}
