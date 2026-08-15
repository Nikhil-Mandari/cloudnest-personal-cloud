import { motion } from 'framer-motion';
import { Check, FolderOpen, RotateCcw, Trash2 } from 'lucide-react';

import { FileIcon } from '@/components/files/FileIcon';
import type { SortDirection } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { formatBytes } from '@/utils/format';
import type { TrashEntry, TrashKind, TrashSortKey } from '@/utils/trash';

export interface TrashTableProps {
  entries: TrashEntry[];
  selectedKeys: ReadonlySet<string>;
  sortKey: TrashSortKey;
  sortDirection: SortDirection;
  onSortChange: (key: TrashSortKey) => void;
  onToggleSelect: (key: string) => void;
  onToggleSelectAll: () => void;
  onRestore: (entry: TrashEntry) => void;
  onDeleteForever: (entry: TrashEntry) => void;
}

const KIND_BADGES: Record<TrashKind, string> = {
  file: 'bg-sky-500/10 text-sky-600 dark:text-sky-400',
  folder: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
};

interface SortableHeaderProps {
  label: string;
  value: TrashSortKey;
  current: TrashSortKey;
  direction: SortDirection;
  onSortChange: (key: TrashSortKey) => void;
  className?: string;
}

function SortableHeader({
  label,
  value,
  current,
  direction,
  onSortChange,
  className,
}: SortableHeaderProps) {
  const active = current === value;

  return (
    <th scope="col" className={cn('px-4 py-3', className)}>
      <button
        type="button"
        onClick={() => onSortChange(value)}
        className={cn(
          'flex items-center gap-1 text-left text-[11px] font-semibold tracking-wider uppercase transition-colors',
          active
            ? 'text-brand-600 dark:text-brand-400'
            : 'text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300',
        )}
      >
        {label}
        <span className="text-[9px]">
          {active ? (direction === 'asc' ? '↑' : '↓') : ''}
        </span>
      </button>
    </th>
  );
}

/** Unified trash listing: files and folders with batch selection + actions. */
export function TrashTable({
  entries,
  selectedKeys,
  sortKey,
  sortDirection,
  onSortChange,
  onToggleSelect,
  onToggleSelectAll,
  onRestore,
  onDeleteForever,
}: TrashTableProps) {
  const allSelected = entries.length > 0 && entries.every((entry) => selectedKeys.has(entry.key));
  const someSelected = entries.some((entry) => selectedKeys.has(entry.key));

  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200/80 bg-white shadow-sm dark:border-gray-800 dark:bg-gray-900">
      <table className="w-full">
        <thead className="border-b border-gray-100 bg-gray-50/60 dark:border-gray-800 dark:bg-gray-800/30">
          <tr>
            <th scope="col" className="w-12 px-4 py-3">
              <button
                type="button"
                role="checkbox"
                aria-checked={someSelected ? 'mixed' : allSelected}
                aria-label="Select all items"
                onClick={onToggleSelectAll}
                className={cn(
                  'grid h-5 w-5 place-items-center rounded-md border transition-colors',
                  allSelected
                    ? 'border-brand-500 bg-brand-500 text-white'
                    : 'border-gray-300 bg-white dark:border-gray-600 dark:bg-gray-900',
                )}
              >
                {allSelected && <Check className="h-3.5 w-3.5" />}
              </button>
            </th>
            <SortableHeader
              label="Name"
              value="name"
              current={sortKey}
              direction={sortDirection}
              onSortChange={onSortChange}
            />
            <th scope="col" className="hidden px-4 py-3 text-[11px] font-semibold tracking-wider text-gray-400 uppercase sm:table-cell dark:text-gray-500">
              Type
            </th>
            <th scope="col" className="hidden px-4 py-3 text-[11px] font-semibold tracking-wider text-gray-400 uppercase md:table-cell dark:text-gray-500">
              Size
            </th>
            <SortableHeader
              label="Deleted"
              value="date"
              current={sortKey}
              direction={sortDirection}
              onSortChange={onSortChange}
              className="hidden md:table-cell"
            />
            <th scope="col" className="w-24 px-4 py-3" />
          </tr>
        </thead>

        <tbody>
          {entries.map((entry, index) => {
            const selected = selectedKeys.has(entry.key);
            return (
              <motion.tr
                key={entry.key}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.15, delay: Math.min(index * 0.02, 0.2) }}
                className={cn(
                  'border-b border-gray-100 transition-colors last:border-0 dark:border-gray-800/70',
                  selected
                    ? 'bg-brand-500/[0.06] dark:bg-brand-400/[0.06]'
                    : 'hover:bg-gray-50 dark:hover:bg-gray-800/40',
                )}
              >
                <td className="px-4 py-3">
                  <button
                    type="button"
                    role="checkbox"
                    aria-checked={selected}
                    aria-label={`Select ${entry.name}`}
                    onClick={() => onToggleSelect(entry.key)}
                    className={cn(
                      'grid h-5 w-5 place-items-center rounded-md border transition-colors',
                      selected
                        ? 'border-brand-500 bg-brand-500 text-white'
                        : 'border-gray-300 bg-white dark:border-gray-600 dark:bg-gray-900',
                    )}
                  >
                    {selected && <Check className="h-3.5 w-3.5" />}
                  </button>
                </td>

                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    {entry.kind === 'file' && entry.file ? (
                      <FileIcon file={entry.file} size="sm" />
                    ) : (
                      <span className="bg-amber-500/10 text-amber-600 dark:text-amber-400 grid h-8 w-8 shrink-0 place-items-center rounded-lg">
                        <FolderOpen className="h-4 w-4" />
                      </span>
                    )}
                    <span
                      title={entry.name}
                      className="max-w-64 truncate text-sm font-medium text-gray-900 dark:text-white"
                    >
                      {entry.name}
                    </span>
                  </div>
                </td>

                <td className="hidden px-4 py-3 sm:table-cell">
                  <span
                    className={cn(
                      'inline-flex rounded-full px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wide',
                      KIND_BADGES[entry.kind],
                    )}
                  >
                    {entry.kind}
                  </span>
                </td>

                <td className="hidden px-4 py-3 text-sm text-gray-500 md:table-cell dark:text-gray-400">
                  {entry.kind === 'file' ? formatBytes(entry.size) : '—'}
                </td>

                <td className="hidden px-4 py-3 text-sm text-gray-500 md:table-cell dark:text-gray-400">
                  {formatFileDate(entry.deletedAt)}
                </td>

                <td className="px-4 py-3">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => onRestore(entry)}
                      aria-label={`Restore ${entry.name}`}
                      title="Restore"
                      className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-emerald-50 hover:text-emerald-600 dark:text-gray-500 dark:hover:bg-emerald-500/10 dark:hover:text-emerald-400"
                    >
                      <RotateCcw className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeleteForever(entry)}
                      aria-label={`Permanently delete ${entry.name}`}
                      title="Delete forever"
                      className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-rose-50 hover:text-rose-600 dark:text-gray-500 dark:hover:bg-rose-500/10 dark:hover:text-rose-400"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </motion.tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
