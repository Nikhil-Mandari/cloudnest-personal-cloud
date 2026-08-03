/**
 * File explorer UI state types (view mode, sorting, filtering).
 */

export type FileViewMode = 'grid' | 'list';

export type SortKey = 'name' | 'size' | 'date' | 'type';

export type SortDirection = 'asc' | 'desc';

export type FileTypeCategory =
  'image' | 'document' | 'video' | 'audio' | 'archive' | 'code' | 'other';

/** `all` = everything, `favorites` = starred files, otherwise a category. */
export type FileTypeFilter = 'all' | 'favorites' | FileTypeCategory;

export interface SortState {
  key: SortKey;
  direction: SortDirection;
}
