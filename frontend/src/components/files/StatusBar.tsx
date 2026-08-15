import { Command, Files } from 'lucide-react';

export interface StatusBarProps {
  /** Total files at the current location (before search/filter). */
  itemCount: number;
  /** Number of folders at the current location. */
  folderCount: number;
  /** Number of currently selected files. */
  selectedCount: number;
}

/** Explorer status bar (Windows Explorer / Finder style). */
export function StatusBar({ itemCount, folderCount, selectedCount }: StatusBarProps) {
  return (
    <div className="flex items-center gap-4 border-t border-gray-200/80 px-1 pt-3 text-[11px] text-gray-400 dark:border-gray-800 dark:text-gray-500">
      <span className="flex items-center gap-1.5">
        <Files className="h-3.5 w-3.5" />
        {itemCount} file{itemCount === 1 ? '' : 's'}
      </span>
      {folderCount > 0 && <span>{folderCount} folder{folderCount === 1 ? '' : 's'}</span>}
      {selectedCount > 0 && (
        <span className="font-medium text-brand-600 dark:text-brand-400">
          {selectedCount} selected
        </span>
      )}

      <span className="ml-auto hidden items-center gap-1 sm:flex">
        <Command className="h-3 w-3" />
        <span className="rounded border border-gray-200 px-1 dark:border-gray-700">↑↓</span> move
        <span className="rounded border border-gray-200 px-1 dark:border-gray-700">↵</span> open
        <span className="rounded border border-gray-200 px-1 dark:border-gray-700">F2</span> rename
        <span className="rounded border border-gray-200 px-1 dark:border-gray-700">Del</span> trash
      </span>
    </div>
  );
}
