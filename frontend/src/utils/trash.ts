import type { FileItem, Folder, SortDirection } from '@/types';

export type TrashSortKey = 'name' | 'date';

export type TrashKind = 'file' | 'folder';

/**
 * Unified view of a trashed item. The `key` is stable and unique across both
 * files and folders (used for selection).
 */
export interface TrashEntry {
  key: string;
  kind: TrashKind;
  name: string;
  /** When the item was moved to the trash (metadata `updatedAt`). */
  deletedAt: string;
  /** Size in bytes (folders report 0). */
  size: number;
  file?: FileItem;
  folder?: Folder;
}

export interface TrashData {
  files: FileItem[];
  folders: Folder[];
}

/** Merges trashed files + folders into a single sortable, selectable list. */
export function buildTrashEntries({ files, folders }: TrashData): TrashEntry[] {
  return [
    ...folders.map(
      (folder): TrashEntry => ({
        key: `folder:${folder.id}`,
        kind: 'folder',
        name: folder.name,
        deletedAt: folder.updatedAt,
        size: 0,
        folder,
      }),
    ),
    ...files.map(
      (file): TrashEntry => ({
        key: `file:${file.id}`,
        kind: 'file',
        name: file.originalFileName,
        deletedAt: file.updatedAt,
        size: file.fileSize,
        file,
      }),
    ),
  ];
}

/** Stable, deterministic sort for trash entries (never mutates input). */
export function sortTrashEntries(
  entries: TrashEntry[],
  key: TrashSortKey,
  direction: SortDirection,
): TrashEntry[] {
  const dir = direction === 'asc' ? 1 : -1;

  return [...entries].sort((a, b) => {
    if (key === 'name') {
      return a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' }) * dir;
    }
    return (new Date(a.deletedAt).getTime() - new Date(b.deletedAt).getTime()) * dir;
  });
}

/** Applies the search query against item names. */
export function filterTrashEntries(entries: TrashEntry[], query: string): TrashEntry[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return entries;
  }
  return entries.filter((entry) => entry.name.toLowerCase().includes(q));
}
