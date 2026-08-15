import { useCallback, useEffect, useRef, useState } from 'react';
import { Scan, ZoomIn, ZoomOut } from 'lucide-react';

import { cn } from '@/utils/cn';

const MIN_SCALE = 0.25;
const MAX_SCALE = 6;
const ZOOM_STEP = 1.4;

interface Transform {
  scale: number;
  x: number;
  y: number;
}

const clampScale = (scale: number): number => Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));

export interface ImagePreviewProps {
  src: string;
  alt: string;
}

const iconButtonClasses =
  'grid h-8 w-8 place-items-center rounded-full text-gray-600 transition-colors hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800';

/**
 * Pan-and-zoom image viewer: wheel / buttons / `+` `-` `0` keys zoom toward
 * the cursor, drag pans when zoomed in, double-click toggles 2×, and the image
 * re-fits whenever the stage resizes (window resize, fullscreen toggle).
 */
export function ImagePreview({ src, alt }: ImagePreviewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLImageElement>(null);
  const transformRef = useRef<Transform>({ scale: 1, x: 0, y: 0 });
  const dragRef = useRef<{ startX: number; startY: number; tx: number; ty: number } | null>(null);

  const [transform, setTransformState] = useState<Transform>({ scale: 1, x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);

  const applyTransform = useCallback((next: Transform) => {
    transformRef.current = next;
    setTransformState(next);
  }, []);

  /** Fits the image to the stage (contain, never upscaled past 100%). */
  const fit = useCallback(() => {
    const container = containerRef.current;
    const image = imgRef.current;
    if (!container || !image) {
      return;
    }
    const rect = container.getBoundingClientRect();
    const { naturalWidth, naturalHeight } = image;
    if (!naturalWidth || !naturalHeight || rect.width === 0 || rect.height === 0) {
      return;
    }
    const scale = clampScale(Math.min(rect.width / naturalWidth, rect.height / naturalHeight, 1));
    applyTransform({ scale, x: 0, y: 0 });
  }, [applyTransform]);

  /** Zooms by `factor`, keeping the image point under (px, py) stationary. */
  const zoomAt = useCallback(
    (px: number, py: number, factor: number) => {
      const current = transformRef.current;
      const nextScale = clampScale(current.scale * factor);
      const imageX = (px - current.x) / current.scale;
      const imageY = (py - current.y) / current.scale;
      applyTransform({ scale: nextScale, x: px - imageX * nextScale, y: py - imageY * nextScale });
    },
    [applyTransform],
  );

  /** Zooms toward the stage centre (buttons / keyboard). */
  const zoomBy = useCallback(
    (factor: number) => {
      const container = containerRef.current;
      if (!container) {
        return;
      }
      const rect = container.getBoundingClientRect();
      zoomAt(rect.width / 2, rect.height / 2, factor);
    },
    [zoomAt],
  );

  // Non-passive wheel listener so preventDefault reliably stops page scroll.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      const rect = container.getBoundingClientRect();
      const factor = event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
      zoomAt(event.clientX - rect.left, event.clientY - rect.top, factor);
    };
    container.addEventListener('wheel', onWheel, { passive: false });
    return () => container.removeEventListener('wheel', onWheel);
  }, [zoomAt]);

  // Re-fit whenever the stage resizes (window resize, fullscreen, first
  // layout) — but only while the user hasn't zoomed in.
  useEffect(() => {
    const container = containerRef.current;
    if (!container || typeof ResizeObserver === 'undefined') {
      return;
    }
    const observer = new ResizeObserver(() => {
      if (transformRef.current.scale <= 1.001) {
        fit();
      }
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, [fit]);

  // Keyboard zoom shortcuts (+ / - / 0). The explorer shortcuts are disabled
  // while the preview is open, so nothing else competes for these keys.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (
        target?.tagName === 'INPUT' ||
        target?.tagName === 'TEXTAREA' ||
        target?.isContentEditable
      ) {
        return;
      }
      // Leave modified shortcuts (Ctrl+/-/0 = browser zoom) alone.
      if (event.ctrlKey || event.metaKey) {
        return;
      }
      if (event.key === '+' || event.key === '=') {
        event.preventDefault();
        zoomBy(ZOOM_STEP);
      } else if (event.key === '-' || event.key === '_') {
        event.preventDefault();
        zoomBy(1 / ZOOM_STEP);
      } else if (event.key === '0') {
        event.preventDefault();
        fit();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [zoomBy, fit]);

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (transformRef.current.scale <= 1.001) {
      return;
    }
    event.preventDefault();
    dragRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      tx: transformRef.current.x,
      ty: transformRef.current.y,
    };
    setIsDragging(true);
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  /** Keeps a panned axis inside the stage so the image can never be lost. */
  const clampAxis = (value: number, visual: number, visible: number): number => {
    const max = Math.max((visual - visible) / 2, 0);
    return Math.min(Math.max(value, -max), max);
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current;
    if (!drag) {
      return;
    }
    const current = transformRef.current;
    let nextX = drag.tx + (event.clientX - drag.startX);
    let nextY = drag.ty + (event.clientY - drag.startY);

    const container = containerRef.current;
    const image = imgRef.current;
    if (container && image) {
      const rect = container.getBoundingClientRect();
      const layoutW = Math.min(image.naturalWidth || 0, rect.width);
      const layoutH = Math.min(image.naturalHeight || 0, rect.height);
      nextX = clampAxis(nextX, layoutW * current.scale, rect.width);
      nextY = clampAxis(nextY, layoutH * current.scale, rect.height);
    }

    applyTransform({ ...current, x: nextX, y: nextY });
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current) {
      return;
    }
    dragRef.current = null;
    setIsDragging(false);
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  };

  const handleDoubleClick = (event: React.MouseEvent<HTMLDivElement>) => {
    const current = transformRef.current;
    if (current.scale > 1.001) {
      fit();
      return;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    zoomAt(event.clientX - rect.left, event.clientY - rect.top, 2);
  };

  return (
    <div
      ref={containerRef}
      className={cn(
        'relative flex h-full w-full touch-none items-center justify-center overflow-hidden select-none',
        isDragging
          ? 'cursor-grabbing'
          : transform.scale > 1.001
            ? 'cursor-grab'
            : 'cursor-zoom-in',
      )}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      onDoubleClick={handleDoubleClick}
    >
      <img
        ref={imgRef}
        src={src}
        alt={alt}
        draggable={false}
        onLoad={fit}
        onDragStart={(event) => event.preventDefault()}
        className="max-h-full max-w-full rounded-lg object-contain shadow-sm select-none will-change-transform"
        style={{
          transform: `translate3d(${transform.x}px, ${transform.y}px, 0) scale(${transform.scale})`,
        }}
      />

      {/* Zoom controls */}
      <div className="absolute bottom-3 left-1/2 z-10 flex -translate-x-1/2 items-center gap-0.5 rounded-full border border-gray-200 bg-white/90 p-1 shadow-lg backdrop-blur-sm dark:border-gray-700 dark:bg-gray-900/90">
        <button
          type="button"
          onClick={() => zoomBy(1 / ZOOM_STEP)}
          aria-label="Zoom out"
          className={iconButtonClasses}
        >
          <ZoomOut className="h-4 w-4" />
        </button>
        <button
          type="button"
          onClick={fit}
          aria-label="Reset zoom"
          title="Reset zoom"
          className="min-w-12 rounded-full px-2 text-xs font-semibold text-gray-700 tabular-nums hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-800"
        >
          {Math.round(transform.scale * 100)}%
        </button>
        <button
          type="button"
          onClick={() => zoomBy(ZOOM_STEP)}
          aria-label="Zoom in"
          className={iconButtonClasses}
        >
          <ZoomIn className="h-4 w-4" />
        </button>
        <span aria-hidden="true" className="mx-0.5 h-4 w-px bg-gray-200 dark:bg-gray-700" />
        <button
          type="button"
          onClick={fit}
          aria-label="Fit to screen"
          title="Fit to screen"
          className={iconButtonClasses}
        >
          <Scan className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
