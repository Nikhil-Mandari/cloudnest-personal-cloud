import type { Folder, FolderSortKey, SortDirection } from '@/types';

/**
 * Folder domain helpers: naming validation, sorting and filtering.
 */

const INVALID_NAME_CHARS = '<>:"/\\|?*';
const MAX_NAME_LENGTH = 255;

/** Rejects forbidden characters and control characters. */
function hasInvalidNameChars(name: string): boolean {
  for (const char of name) {
    const code = char.codePointAt(0) ?? 0;
    if (code < 32 || INVALID_NAME_CHARS.includes(char)) {
      return true;
    }
  }
  return false;
}

/**
 * Validates a folder name, returning a human-readable error message or
 * `undefined` when the name is acceptable.
 */
export function validateFolderName(name: string): string | undefined {
  const trimmed = name.trim();
  if (!trimmed) {
    return 'Folder name cannot be empty.';
  }
  if (trimmed.length > MAX_NAME_LENGTH) {
    return `Folder name is too long (max ${MAX_NAME_LENGTH} characters).`;
  }
  if (hasInvalidNameChars(trimmed)) {
    return 'Name contains invalid characters.';
  }
  return undefined;
}

/** Stable, deterministic sort used by the folder explorer (never mutates input). */
export function sortFolders(
  folders: Folder[],
  key: FolderSortKey,
  direction: SortDirection,
): Folder[] {
  const dir = direction === 'asc' ? 1 : -1;

  return [...folders].sort((a, b) => {
    switch (key) {
      case 'name':
        return (
          a.name.localeCompare(b.name, undefined, {
            numeric: true,
            sensitivity: 'base',
          }) * dir
        );
      case 'date':
        return (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) * dir;
      default:
        return 0;
    }
  });
}

/** Applies the search query to a folder list (case-insensitive name match). */
export function filterFolders(folders: Folder[], searchQuery: string): Folder[] {
  const query = searchQuery.trim().toLowerCase();

  if (!query) {
    return folders;
  }

  return folders.filter((folder) => folder.name.toLowerCase().includes(query));
}
