import type { ShareRecord } from '@/types';
import { MySharesRow } from './MySharesRow';

export interface MySharesTableProps {
  shares: ShareRecord[];
  onCopyLink: (share: ShareRecord) => void;
  onOpenSettings: (share: ShareRecord) => void;
  onRevoke: (share: ShareRecord) => void;
}

/** Table of the user's own share links (owner view with analytics counters). */
export function MySharesTable({
  shares,
  onCopyLink,
  onOpenSettings,
  onRevoke,
}: MySharesTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200/80 bg-white shadow-sm shadow-gray-900/[0.03] dark:border-gray-800 dark:bg-gray-900">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b border-gray-100 dark:border-gray-800">
              <th
                scope="col"
                className="px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400"
              >
                Resource
              </th>
              <th
                scope="col"
                className="px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400"
              >
                Permission
              </th>
              <th
                scope="col"
                className="px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400"
              >
                Link status
              </th>
              <th
                scope="col"
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase md:table-cell dark:text-gray-400"
              >
                Views
              </th>
              <th
                scope="col"
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase lg:table-cell dark:text-gray-400"
              >
                Downloads
              </th>
              <th
                scope="col"
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase xl:table-cell dark:text-gray-400"
              >
                Last access
              </th>
              <th
                scope="col"
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase 2xl:table-cell dark:text-gray-400"
              >
                Shared
              </th>
              <th scope="col" className="w-28 py-3 pr-4" aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {shares.map((share) => (
              <MySharesRow
                key={share.id}
                share={share}
                onCopyLink={onCopyLink}
                onOpenSettings={onOpenSettings}
                onRevoke={onRevoke}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
