import { Suspense, useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Outlet, useLocation } from 'react-router-dom';
import { Share2, ShieldCheck, Zap } from 'lucide-react';

import { Brand } from '@/components/common/Brand';
import { Loader } from '@/components/common/Loader';
import { Card } from '@/components/ui/Card';

const FEATURES = [
  { icon: ShieldCheck, text: 'Secure end-to-end file storage' },
  { icon: Zap, text: 'Fast uploads & smart search' },
  { icon: Share2, text: 'Share files with a single link' },
];

export function AuthLayout() {
  const location = useLocation();

  // New page = new scroll position (consistent with the dashboard layout).
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <div className="flex min-h-screen bg-gray-50 dark:bg-gray-950">
      {/* Brand panel (desktop only) */}
      <div className="from-brand-600 via-brand-700 to-accent-700 relative hidden w-[45%] flex-col justify-between overflow-hidden bg-linear-to-br p-12 text-white lg:flex">
        <div className="pointer-events-none absolute inset-0">
          <div className="animate-float absolute -top-20 -left-20 h-72 w-72 rounded-full bg-white/10 blur-3xl" />
          <div className="animate-float-slow bg-accent-500/20 absolute -right-10 bottom-0 h-96 w-96 rounded-full blur-3xl" />
          <div className="animate-float bg-brand-400/20 absolute top-1/2 left-1/3 h-64 w-64 rounded-full blur-3xl" />
        </div>

        <div className="relative z-10">
          <Brand inverted />
        </div>

        <div className="relative z-10 space-y-8">
          <h1 className="text-4xl leading-tight font-bold">
            Your files.
            <br />
            Your cloud.
            <br />
            <span className="text-white/80">Anywhere.</span>
          </h1>

          <ul className="space-y-3 text-sm text-white/80">
            {FEATURES.map((feature) => (
              <li key={feature.text} className="flex items-center gap-3">
                <feature.icon className="h-5 w-5 text-white/90" />
                {feature.text}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative z-10 text-sm text-white/60">
          © {new Date().getFullYear()} CloudNest · Personal Cloud
        </p>
      </div>

      {/* Form panel */}
      <div className="relative flex flex-1 items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-md">
          <div className="mb-8 flex justify-center lg:hidden">
            <Brand />
          </div>

          {/* Animated route transitions — same exit/enter feel as the dashboard. */}
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 12, scale: 0.99 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8, scale: 0.99 }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
            >
              <Suspense fallback={<Loader className="py-24" />}>
                <Card className="p-7 shadow-xl shadow-gray-900/5 sm:p-9">
                  <Outlet />
                </Card>
              </Suspense>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
