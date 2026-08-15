import { Menu } from 'lucide-react';

import { useUiStore } from '@/store/uiStore';
import { GlobalSearch } from './GlobalSearch';
import { NotificationBell } from './NotificationBell';
import { ThemeToggle } from './ThemeToggle';
import { UserMenu } from './UserMenu';

export function Navbar() {
  const openMobileSidebar = useUiStore((state) => state.openMobileSidebar);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-gray-200/70 bg-white/80 px-4 backdrop-blur-xl sm:px-6 lg:px-8 dark:border-gray-800 dark:bg-gray-950/70">
      <button
        type="button"
        onClick={openMobileSidebar}
        aria-label="Open navigation menu"
        className="grid h-10 w-10 place-items-center rounded-lg text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 lg:hidden dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* Global search — searches files (server-side) and folders across the workspace */}
      <div className="hidden md:block md:max-w-md md:flex-1">
        <GlobalSearch />
      </div>

      <div className="ml-auto flex items-center gap-1.5">
        <ThemeToggle />
        <NotificationBell />
        <UserMenu />
      </div>
    </header>
  );
}
