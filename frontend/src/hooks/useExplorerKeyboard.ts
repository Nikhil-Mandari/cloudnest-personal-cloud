import { useEffect, useRef } from 'react';

import type { FileItem } from '@/types';

export interface UseExplorerKeyboardOptions {
  /** Disable while a dialog is open so F2/Delete never fire through it. */
  enabled?: boolean;
  /** Ordered, visible file list — arrow navigation moves through it. */
  files: FileItem[];
  selectedIds: number[];
  /** Replaces the selection with the given file (plain select). */
  onSelectFile: (file: FileItem) => void;
  onOpenPreview: (file: FileItem) => void;
  onDelete: (file: FileItem) => void;
  onRename: (file: FileItem) => void;
  onSelectAll: () => void;
  /** Escape — clears the selection / closes the details panel. */
  onEscape?: () => void;
  /** Alt+↑ — navigate to the parent folder. */
  onGoUp?: () => void;
}

const isTyping = (target: EventTarget | null): boolean => {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  return (
    target.tagName === 'INPUT' ||
    target.tagName === 'TEXTAREA' ||
    target.isContentEditable ||
    target.tagName === 'SELECT'
  );
};

/**
 * File-manager keyboard shortcuts for the explorer: arrow navigation through
 * the visible files, Enter (preview), Delete (trash), F2 (rename), Ctrl/Cmd+A
 * (select all), Escape (clear) and Alt+↑ (parent folder).
 */
export function useExplorerKeyboard(options: UseExplorerKeyboardOptions) {
  const optionsRef = useRef(options);

  // Keep the ref pointing at the latest options without re-registering the
  // window listener (refs are safe to mutate from an effect, not from render).
  useEffect(() => {
    optionsRef.current = options;
  });

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const {
        enabled = true,
        files,
        selectedIds,
        onSelectFile,
        onOpenPreview,
        onDelete,
        onRename,
        onSelectAll,
        onEscape,
        onGoUp,
      } = optionsRef.current;

      if (!enabled || files.length === 0 || isTyping(event.target)) {
        return;
      }

      const mod = event.ctrlKey || event.metaKey;

      // Arrow navigation through the visible list (wraps at the edges).
      if (
        event.key === 'ArrowDown' ||
        event.key === 'ArrowUp' ||
        event.key === 'ArrowRight' ||
        event.key === 'ArrowLeft'
      ) {
        event.preventDefault();
        const direction = event.key === 'ArrowDown' || event.key === 'ArrowRight' ? 1 : -1;
        const lastId = selectedIds[selectedIds.length - 1];
        const currentIndex = files.findIndex((file) => file.id === lastId);
        const nextIndex =
          currentIndex === -1 ? 0 : (currentIndex + direction + files.length) % files.length;
        onSelectFile(files[nextIndex]);
        return;
      }

      if (event.key === 'Enter') {
        const lastId = selectedIds[selectedIds.length - 1];
        const file = files.find((item) => item.id === lastId);
        if (file) {
          event.preventDefault();
          onOpenPreview(file);
        }
        return;
      }

      if (event.key === 'Delete') {
        const lastId = selectedIds[selectedIds.length - 1];
        const file = files.find((item) => item.id === lastId);
        if (file) {
          event.preventDefault();
          onDelete(file);
        }
        return;
      }

      if (event.key === 'F2') {
        const lastId = selectedIds[selectedIds.length - 1];
        const file = files.find((item) => item.id === lastId);
        if (file) {
          event.preventDefault();
          onRename(file);
        }
        return;
      }

      if (mod && event.key.toLowerCase() === 'a') {
        event.preventDefault();
        onSelectAll();
        return;
      }

      if (event.key === 'Escape') {
        onEscape?.();
        return;
      }

      // Alt+↑ (Windows Explorer / macOS ⌘↑) → parent folder.
      if (event.altKey && event.key === 'ArrowUp') {
        event.preventDefault();
        onGoUp?.();
        return;
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);
}
