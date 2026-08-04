import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';

import { cn } from '@/utils/cn';

export interface ContextMenuItem {
  key: string;
  label: string;
  icon?: ReactNode;
  danger?: boolean;
  disabled?: boolean;
  /** Renders a divider row instead of a button. */
  separator?: boolean;
  onClick?: () => void;
}

export interface ContextMenuPosition {
  x: number;
  y: number;
}

export interface FileContextMenuProps {
  open: boolean;
  position: ContextMenuPosition;
  items: ContextMenuItem[];
  onClose: () => void;
}

const MENU_PADDING = 8;

/**
 * Right-click context menu for files. Rendered through a portal, clamped to
 * the viewport, closed by outside click / scroll / resize / Escape.
 */
export function FileContextMenu({ open, position, items, onClose }: FileContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const [coords, setCoords] = useState(position);

  // Clamp the menu inside the viewport once it has been measured.
  useLayoutEffect(() => {
    if (!open) {
      return;
    }
    const element = menuRef.current;
    if (!element) {
      return;
    }
    const rect = element.getBoundingClientRect();
    const x = Math.max(
      MENU_PADDING,
      Math.min(position.x, window.innerWidth - rect.width - MENU_PADDING),
    );
    const y = Math.max(
      MENU_PADDING,
      Math.min(position.y, window.innerHeight - rect.height - MENU_PADDING),
    );
    setCoords({ x, y });
  }, [open, position]);

  // Close on Escape, scroll, resize, window blur or another context menu.
  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('scroll', onClose, true);
    window.addEventListener('resize', onClose);
    window.addEventListener('blur', onClose);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('scroll', onClose, true);
      window.removeEventListener('resize', onClose);
      window.removeEventListener('blur', onClose);
    };
  }, [open, onClose]);

  if (typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <AnimatePresence>
      {open && (
        <>
          {/* Dismissal overlay — captures clicks and right-clicks outside the menu */}
          <div
            className="fixed inset-0 z-40"
            onMouseDown={onClose}
            onContextMenu={onClose}
            aria-hidden="true"
          />
          <motion.div
            ref={menuRef}
            role="menu"
            aria-label="File actions"
            className="fixed z-50 w-60 overflow-hidden rounded-xl border border-gray-200 bg-white py-1.5 shadow-xl shadow-gray-900/10 focus:outline-none dark:border-gray-700 dark:bg-gray-900 dark:shadow-black/40"
            style={{ left: coords.x, top: coords.y }}
            initial={{ opacity: 0, scale: 0.95, y: -6 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -6 }}
            transition={{ duration: 0.14, ease: 'easeOut' }}
          >
            {items.map((item) =>
              item.separator ? (
                <div
                  key={item.key}
                  role="separator"
                  className="my-1.5 h-px bg-gray-100 dark:bg-gray-800"
                />
              ) : (
                <button
                  key={item.key}
                  type="button"
                  role="menuitem"
                  disabled={item.disabled}
                  onClick={item.onClick}
                  className={cn(
                    'flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors',
                    item.danger
                      ? 'text-rose-600 hover:bg-rose-500/10 dark:text-rose-400'
                      : 'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-800/70',
                    item.disabled && 'pointer-events-none opacity-50',
                  )}
                >
                  {item.icon && (
                    <span
                      className={cn(
                        'grid h-4 w-4 shrink-0 place-items-center',
                        item.danger && 'text-rose-500 dark:text-rose-400',
                      )}
                    >
                      {item.icon}
                    </span>
                  )}
                  {item.label}
                </button>
              ),
            )}
          </motion.div>
        </>
      )}
    </AnimatePresence>,
    document.body,
  );
}
