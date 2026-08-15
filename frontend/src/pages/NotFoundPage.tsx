import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, CloudOff } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { APP_ROUTES } from '@/constants/routes';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gray-50 px-6 text-center dark:bg-gray-950">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-float bg-brand-500/10 absolute top-10 -left-24 h-72 w-72 rounded-full blur-3xl" />
        <div className="animate-float-slow bg-accent-500/10 absolute -right-24 bottom-10 h-80 w-80 rounded-full blur-3xl" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: 'easeOut' }}
        className="relative z-10"
      >
        <p className="from-brand-600 to-accent-600 bg-linear-to-r bg-clip-text text-8xl font-extrabold tracking-tight text-transparent">
          404
        </p>
        <CloudOff className="mx-auto mt-4 h-12 w-12 text-gray-300 dark:text-gray-700" />
        <h1 className="mt-4 text-2xl font-bold text-gray-900 dark:text-white">Page not found</h1>
        <p className="mx-auto mt-2 max-w-md text-sm text-gray-500 dark:text-gray-400">
          The page you&apos;re looking for doesn&apos;t exist or has been moved.
        </p>

        <div className="mt-8 flex justify-center gap-3">
          <Button
            onClick={() => navigate(APP_ROUTES.dashboard)}
            leftIcon={<ArrowLeft className="h-4 w-4" />}
          >
            Back to dashboard
          </Button>
          <Button variant="outline" onClick={() => navigate(APP_ROUTES.login)}>
            Sign in
          </Button>
        </div>
      </motion.div>
    </div>
  );
}
