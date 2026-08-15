/**
 * File explorer UI state types (view mode, sorting, filtering).
 */

export type FileViewMode = 'grid' | 'list' | 'compact';

export type SortKey = 'name' | 'size' | 'date' | 'type';

export type SortDirection = 'asc' | 'desc';

export type FileTypeCategory =
  | 'image'
  | 'document'
  | 'video'
  | 'audio'
  | 'archive'
  | 'code'
  | 'other';

/**
 * `all` = everything, `favorites` = starred files, otherwise a category.
 * `pdf` = PDF documents, `recent` = uploaded within the last 7 days,
 * `shared` = files the user has created a share link for.
 */
export type FileTypeFilter =
  | 'all'
  | 'favorites'
  | 'pdf'
  | 'recent'
  | 'shared'
  | FileTypeCategory;

export interface SortState {
  key: SortKey;
  direction: SortDirection;
}
