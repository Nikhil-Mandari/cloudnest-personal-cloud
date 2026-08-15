import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { ShareGrid } from '@/components/shares/ShareGrid';
import { SharesEmptyState } from '@/components/shares/ShareEmptyState';
import { ShareTable } from '@/components/shares/ShareTable';
import { ShareToolbar } from '@/components/shares/ShareToolbar';
import { ShareGridSkeleton, ShareTableSkeleton } from '@/components/shares/ShareSkeletons';
import { APP_ROUTES } from '@/constants/routes';
import { useSharedWithMeQuery } from '@/hooks/useShare';
import type {
  FileViewMode,
  ShareRecord,
  ShareSortKey,
  ShareTypeFilter,
  SortDirection,
} from '@/types';
import { copyToClipboard } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';
import { buildShareUrl } from '@/utils/file';
import { filterShares, sortShares } from '@/utils/share';

export function SharedPage() {
  const { data: shares = [], isLoading, isError, error, refetch } = useSharedWithMeQuery();
  const navigate = useNavigate();

  // Explorer UI state (transient — no persistence needed for this view).
  const [viewMode, setViewMode] = useState<FileViewMode>('list');
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<ShareTypeFilter>('all');
  const [sortKey, setSortKey] = useState<ShareSortKey>('date');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const visibleShares = useMemo(
    () => sortShares(filterShares(shares, searchQuery, typeFilter), sortKey, sortDirection),
    [shares, searchQuery, typeFilter, sortKey, sortDirection],
  );

  const handleSortDirectionChange = () =>
    setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');

  const handleCopyLink = async (share: ShareRecord) => {
    const ok = await copyToClipboard(buildShareUrl(share.shareToken));
    if (ok) {
      toast.success('Share link copied to clipboard');
    } else {
      toast.error('Could not copy the link');
    }
  };

  /** Opens the shared item's page in the current tab (SPA navigation). */
  const handleOpen = (share: ShareRecord) => {
    navigate(APP_ROUTES.publicShare(share.shareToken));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Shared with you"
        description="Files and folders others have shared with you, in one place."
      />

      <ShareToolbar
        resultCount={visibleShares.length}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        typeFilter={typeFilter}
        onTypeFilterChange={setTypeFilter}
        sortKey={sortKey}
        sortDirection={sortDirection}
        onSortKeyChange={setSortKey}
        onSortDirectionChange={handleSortDirectionChange}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
      />

      {/* Content: skeleton → error → empty → grid/table */}
      {isLoading ? (
        viewMode === 'grid' ? (
          <ShareGridSkeleton />
        ) : (
          <ShareTableSkeleton />
        )
      ) : isError ? (
        <ErrorState
          message={getErrorMessage(error, 'Failed to load items shared with you.')}
          onRetry={() => void refetch()}
        />
      ) : visibleShares.length === 0 ? (
        <SharesEmptyState
          variant={shares.length === 0 ? 'no-shares' : 'no-search'}
          searchQuery={searchQuery}
          onClearSearch={() => setSearchQuery('')}
        />
      ) : viewMode === 'grid' ? (
        <ShareGrid
          shares={visibleShares}
          onCopyLink={(share) => void handleCopyLink(share)}
          onOpen={handleOpen}
        />
      ) : (
        <ShareTable
          shares={visibleShares}
          onCopyLink={(share) => void handleCopyLink(share)}
          onOpen={handleOpen}
        />
      )}
    </div>
  );
}
