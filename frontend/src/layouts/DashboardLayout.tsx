import { Suspense } from 'react';
import { motion } from 'framer-motion';
import { Outlet, useLocation } from 'react-router-dom';

import { Footer } from '@/components/layout/Footer';
import { Navbar } from '@/components/layout/Navbar';
import { Sidebar } from '@/components/layout/Sidebar';
import { Loader } from '@/components/common/Loader';
import { useUiStore } from '@/store/uiStore';
import { cn } from '@/utils/cn';

export function DashboardLayout() {
  const location = useLocation();
  const collapsed = useUiStore((state) => state.sidebarCollapsed);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Sidebar />

      <div
        className={cn(
          'flex min-h-screen flex-col transition-[padding] duration-300',
          collapsed ? 'lg:pl-20' : 'lg:pl-64',
        )}
      >
        <Navbar />

        <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 lg:px-8">
          {/* Suspense boundary for lazily-loaded pages + animated transitions */}
          <Suspense fallback={<Loader />}>
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, ease: 'easeOut' }}
            >
              <Outlet />
            </motion.div>
          </Suspense>
        </main>

        <Footer />
      </div>
    </div>
  );
}
