import { Suspense, useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
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

  // New page = new scroll position (feels natural alongside the transition).
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

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
          {/* Animated route transitions — the old page exits (fade + slide up)
              before the new one fades in from below. Lazy pages keep their
              Suspense fallback inside the animated container. */}
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
            >
              <Suspense fallback={<Loader />}>
                <Outlet />
              </Suspense>
            </motion.div>
          </AnimatePresence>
        </main>

        <Footer />
      </div>
    </div>
  );
}
