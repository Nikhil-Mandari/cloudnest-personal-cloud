/** Loading skeleton rows shown while the notifications query is in flight. */

export interface NotificationSkeletonProps {
  count?: number;
}

export function NotificationSkeletons({ count = 5 }: NotificationSkeletonProps) {
  return (
    <div className="divide-y divide-gray-100 dark:divide-gray-800">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="flex animate-pulse items-start gap-3.5 px-5 py-4">
          <div className="h-10 w-10 shrink-0 rounded-xl bg-gray-100 dark:bg-gray-800" />
          <div className="flex-1 space-y-2">
            <div className="h-3.5 w-1/3 rounded-full bg-gray-100 dark:bg-gray-800" />
            <div className="h-3 w-2/3 rounded-full bg-gray-100 dark:bg-gray-800" />
            <div className="h-2.5 w-16 rounded-full bg-gray-100 dark:bg-gray-800" />
          </div>
        </div>
      ))}
    </div>
  );
}
