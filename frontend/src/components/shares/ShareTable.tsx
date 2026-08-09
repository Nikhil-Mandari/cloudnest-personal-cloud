import type { ShareRecord } from '@/types';
import { ShareRow } from './ShareRow';

export interface ShareTableProps {
  shares: ShareRecord[];
  onCopyLink: (share: ShareRecord) => void;
}

/** Responsive table of shared items (grid of metadata rows). */
export function ShareTable({ shares, onCopyLink }: ShareTableProps) {
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
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase md:table-cell dark:text-gray-400"
              >
                Shared
              </th>
              <th
                scope="col"
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase lg:table-cell dark:text-gray-400"
              >
                Expires
              </th>
              <th
                scope="col"
                className="hidden px-3 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase xl:table-cell dark:text-gray-400"
              >
                Owner
              </th>
              <th scope="col" className="w-20 py-3 pr-4" aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {shares.map((share) => (
              <ShareRow key={share.id} share={share} onCopyLink={onCopyLink} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
