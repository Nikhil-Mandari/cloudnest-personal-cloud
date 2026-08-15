/** Loading skeleton rows shown while the trash query is in flight. */

export interface TrashSkeletonProps {
  rows?: number;
}

export function TrashSkeletons({ rows = 6 }: TrashSkeletonProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200/80 bg-white shadow-sm dark:border-gray-800 dark:bg-gray-900">
      <table className="w-full">
        <tbody>
          {Array.from({ length: rows }).map((_, index) => (
            <tr
              key={index}
              className="animate-pulse border-b border-gray-100 last:border-0 dark:border-gray-800/70"
            >
              <td className="py-3.5 pr-0 pl-4">
                <div className="h-4 w-4 rounded-md bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="py-3.5 pr-3">
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-gray-100 dark:bg-gray-800" />
                  <div className="h-3 w-44 rounded-full bg-gray-100 dark:bg-gray-800" />
                </div>
              </td>
              <td className="hidden py-3.5 pr-3 sm:table-cell">
                <div className="h-5 w-14 rounded-full bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="hidden py-3.5 pr-3 md:table-cell">
                <div className="h-3 w-10 rounded-full bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="hidden py-3.5 pr-3 md:table-cell">
                <div className="h-3 w-16 rounded-full bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="w-24 py-3.5 pr-4" />
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
