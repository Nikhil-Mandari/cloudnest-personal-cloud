import type {
  MySharesSortKey,
  ShareRecord,
  ShareSortKey,
  ShareTypeFilter,
  SortDirection,
} from '@/types';

/**
 * Share domain helpers: filtering, sorting and expiry checks for the
 * shared-with-me explorer.
 */

/** Applies the search query and file/folder filter to a share list. */
export function filterShares(
  shares: ShareRecord[],
  searchQuery: string,
  typeFilter: ShareTypeFilter,
): ShareRecord[] {
  const query = searchQuery.trim().toLowerCase();

  return shares.filter((share) => {
    if (typeFilter !== 'all' && share.resourceType !== typeFilter) {
      return false;
    }
    if (query) {
      const haystack = (
        `${share.resourceId} ${share.resourceName ?? ''} ` +
        `${share.resourceType === 'FILE' ? 'file' : 'folder'}`
      ).toLowerCase();
      if (!haystack.includes(query)) {
        return false;
      }
    }
    return true;
  });
}

/** Stable, deterministic sort used by the shared-with-me explorer. */
export function sortShares(
  shares: ShareRecord[],
  key: ShareSortKey,
  direction: SortDirection,
): ShareRecord[] {
  const dir = direction === 'asc' ? 1 : -1;

  return [...shares].sort((a, b) => {
    switch (key) {
      case 'date':
        return (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) * dir;
      case 'type':
        return a.resourceType.localeCompare(b.resourceType) * dir;
      default:
        return 0;
    }
  });
}

/** Whether a share's expiry date has already passed. */
export function isShareExpired(share: ShareRecord): boolean {
  if (!share.expiryDate) {
    return false;
  }
  const expiry = new Date(share.expiryDate).getTime();
  return !Number.isNaN(expiry) && expiry < Date.now();
}

/** Applies a search query against resource names / ids in the My Shares view. */
export function filterMyShares(shares: ShareRecord[], query: string): ShareRecord[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return shares;
  }
  return shares.filter((share) =>
    `${share.resourceName ?? ''} ${share.resourceId} ${share.shareToken}`.toLowerCase().includes(q),
  );
}

/** Stable, deterministic sort for the My Shares management view. */
export function sortMyShares(
  shares: ShareRecord[],
  key: MySharesSortKey,
  direction: SortDirection,
): ShareRecord[] {
  const dir = direction === 'asc' ? 1 : -1;

  return [...shares].sort((a, b) => {
    switch (key) {
      case 'name':
        return (
          (a.resourceName ?? '').localeCompare(b.resourceName ?? '', undefined, {
            numeric: true,
            sensitivity: 'base',
          }) * dir
        );
      case 'views':
        return ((a.viewCount ?? 0) - (b.viewCount ?? 0)) * dir;
      case 'downloads':
        return ((a.downloadCount ?? 0) - (b.downloadCount ?? 0)) * dir;
      case 'date':
      default:
        return (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) * dir;
    }
  });
}
