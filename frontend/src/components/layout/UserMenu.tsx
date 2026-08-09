import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronDown, LogOut, Settings, User } from 'lucide-react';
import { toast } from 'react-toastify';

import { APP_ROUTES } from '@/constants/routes';
import { useClickOutside } from '@/hooks/useClickOutside';
import { useAuth } from '@/hooks/useAuth';
import { getInitials } from '@/utils/format';

export function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useClickOutside(containerRef, () => setOpen(false));

  // NOTE: clean User had fullName; the recovery User contract uses
  // displayName/username (matches the auth-ui unit).
  const displayName = user?.displayName ?? user?.username ?? 'Guest';
  const email = user?.email ?? '';

  const handleLogout = () => {
    logout();
    setOpen(false);
    toast.success('You have been signed out.');
    navigate(APP_ROUTES.login);
  };

  const menuItems = [
    { label: 'Profile', icon: User, onClick: () => navigate(APP_ROUTES.profile) },
    { label: 'Settings', icon: Settings, onClick: () => navigate(APP_ROUTES.settings) },
  ];

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2.5 rounded-lg p-1.5 transition-colors hover:bg-gray-100 dark:hover:bg-gray-800"
      >
        <span className="from-brand-500 to-accent-600 grid h-8 w-8 place-items-center rounded-full bg-linear-to-br text-xs font-bold text-white">
          {getInitials(displayName)}
        </span>
        <span className="hidden text-left sm:block">
          <span className="block max-w-[10rem] truncate text-sm font-medium text-gray-900 dark:text-white">
            {displayName}
          </span>
          <span className="block max-w-[10rem] truncate text-xs text-gray-500 dark:text-gray-400">
            {email}
          </span>
        </span>
        <ChevronDown
          className={`h-4 w-4 text-gray-400 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
        />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="menu"
            className="absolute top-full right-0 z-50 mt-2 w-56 origin-top-right overflow-hidden rounded-xl border border-gray-200 bg-white p-1.5 shadow-xl dark:border-gray-800 dark:bg-gray-900"
            initial={{ opacity: 0, scale: 0.95, y: -6 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -6 }}
            transition={{ duration: 0.15, ease: 'easeOut' }}
          >
            {menuItems.map((item) => (
              <button
                key={item.label}
                type="button"
                role="menuitem"
                onClick={item.onClick}
                className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-800"
              >
                <item.icon className="h-4 w-4 text-gray-400" />
                {item.label}
              </button>
            ))}
            <div className="my-1.5 border-t border-gray-100 dark:border-gray-800" />
            <button
              type="button"
              role="menuitem"
              onClick={handleLogout}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-rose-600 transition-colors hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-500/10"
            >
              <LogOut className="h-4 w-4" />
              Sign out
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
