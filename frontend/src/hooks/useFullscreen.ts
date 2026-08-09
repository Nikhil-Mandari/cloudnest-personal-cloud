import { useCallback, useEffect, useState } from 'react';

/**
 * Fullscreen API helper for a preview element.
 *
 * `supported` reflects browser support, `toggle` enters/exits fullscreen and
 * `isFullscreen` stays in sync with the actual fullscreen state (including
 * exits triggered by the browser's native Escape handling).
 */
export function useFullscreen(element: HTMLElement | null) {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const supported = typeof document !== 'undefined' && document.fullscreenEnabled;

  useEffect(() => {
    if (!element) {
      return;
    }
    const onChange = () => setIsFullscreen(document.fullscreenElement === element);
    document.addEventListener('fullscreenchange', onChange);
    return () => document.removeEventListener('fullscreenchange', onChange);
  }, [element]);

  const toggle = useCallback(async () => {
    if (!element) {
      return;
    }
    try {
      if (document.fullscreenElement) {
        await document.exitFullscreen();
      } else {
        await element.requestFullscreen();
      }
    } catch {
      // Fullscreen was rejected (permission denied / element detached) — no-op.
    }
  }, [element]);

  return { isFullscreen, supported, toggle };
}
