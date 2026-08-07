/**
 * Loading skeletons shown while the files query is in flight.
 */

export interface FileGridSkeletonProps {
  count?: number;
}

export function FileGridSkeleton({ count = 10 }: FileGridSkeletonProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:gap-4 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6">
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className="animate-pulse rounded-2xl border border-gray-200/80 bg-white p-3 dark:border-gray-800 dark:bg-gray-900"
        >
          <div className="mx-auto mt-4 h-14 w-14 rounded-2xl bg-gray-100 dark:bg-gray-800" />
          <div className="mx-auto mt-3 h-3 w-3/4 rounded-full bg-gray-100 dark:bg-gray-800" />
          <div className="mx-auto mt-2 h-2.5 w-1/2 rounded-full bg-gray-100 dark:bg-gray-800" />
          <div className="mt-3 h-px bg-gray-100 dark:bg-gray-800" />
          <div className="mx-auto mt-3 flex items-center justify-center gap-1.5">
            <div className="h-5 w-5 rounded-full bg-gray-100 dark:bg-gray-800" />
            <div className="h-2.5 w-2/3 rounded-full bg-gray-100 dark:bg-gray-800" />
          </div>
        </div>
      ))}
    </div>
  );
}

export interface FileTableSkeletonProps {
  rows?: number;
}

export function FileTableSkeleton({ rows = 6 }: FileTableSkeletonProps) {
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
              <td className="hidden py-3.5 pr-3 md:table-cell">
                <div className="h-3 w-10 rounded-full bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="py-3.5 pr-3">
                <div className="ml-auto h-3 w-14 rounded-full bg-gray-100 sm:ml-0 dark:bg-gray-800" />
              </td>
              <td className="hidden py-3.5 pr-3 lg:table-cell">
                <div className="h-3 w-16 rounded-full bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="hidden py-3.5 pr-3 xl:table-cell">
                <div className="h-3 w-20 rounded-full bg-gray-100 dark:bg-gray-800" />
              </td>
              <td className="w-20 py-3.5 pr-4" />
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
