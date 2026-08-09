import { motion } from 'framer-motion';
import { Clock, Files, FolderOpen, HardDrive, Share2, type LucideIcon } from 'lucide-react';

import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/common/EmptyState';
import { PageHeader } from '@/components/common/PageHeader';
import { useAuth } from '@/hooks/useAuth';
import { formatBytes } from '@/utils/format';

// Sample data — replace with API data once the dashboard is wired to the backend.
const STATS: ReadonlyArray<{ label: string; value: string; trend: string; icon: LucideIcon }> = [
  { label: 'Total Files', value: '1,248', trend: '+12%', icon: Files },
  { label: 'Folders', value: '86', trend: '+4%', icon: FolderOpen },
  { label: 'Shared Links', value: '12', trend: '+2', icon: Share2 },
  { label: 'Storage Used', value: '24.6 GB', trend: 'of 100 GB', icon: HardDrive },
];

const STORAGE_USED_BYTES = 24.6 * 1024 ** 3;
const STORAGE_TOTAL_BYTES = 100 * 1024 ** 3;

export function DashboardPage() {
  const { user } = useAuth();
  // NOTE: clean User had fullName; the recovery User contract uses
  // displayName/username (matches the auth-ui unit).
  const firstName = user?.displayName?.split(' ')[0] ?? 'there';

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description={`Welcome back, ${firstName} 👋 Here's what's happening with your cloud.`}
      />

      {/* Stat cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {STATS.map((stat, index) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.06, duration: 0.3 }}
          >
            <Card className="p-5">
              <div className="flex items-center justify-between">
                <div className="bg-brand-500/10 text-brand-600 dark:bg-brand-400/10 dark:text-brand-300 grid h-11 w-11 place-items-center rounded-xl">
                  <stat.icon className="h-5 w-5" />
                </div>
                <span className="text-xs font-medium text-gray-400 dark:text-gray-500">
                  {stat.trend}
                </span>
              </div>
              <p className="mt-4 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
                {stat.value}
              </p>
              <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{stat.label}</p>
            </Card>
          </motion.div>
        ))}
      </div>

      {/* Recent activity + storage overview */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader
            title="Recent activity"
            description="Your latest file actions will appear here."
          />
          <CardBody>
            <EmptyState
              icon={<Clock className="h-6 w-6" />}
              title="No recent activity yet"
              description="Upload your first file and it will show up here."
            />
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Storage" description="Your free plan usage" />
          <CardBody>
            <div className="h-2.5 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
              <motion.div
                className="from-brand-500 to-accent-500 h-full rounded-full bg-linear-to-r"
                initial={{ width: 0 }}
                animate={{ width: `${(STORAGE_USED_BYTES / STORAGE_TOTAL_BYTES) * 100}%` }}
                transition={{ duration: 0.8, ease: 'easeOut' }}
              />
            </div>
            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="font-medium text-gray-900 dark:text-white">
                {formatBytes(STORAGE_USED_BYTES)}
              </span>
              <span className="text-gray-500 dark:text-gray-400">
                {formatBytes(STORAGE_TOTAL_BYTES)}
              </span>
            </div>
            <p className="mt-4 text-xs text-gray-400 dark:text-gray-500">
              Storage analytics will be connected to the API shortly.
            </p>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
