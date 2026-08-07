import { NavLink } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Bell,
  BarChart3,
  CircleHelp,
  Files,
  FolderOpen,
  LayoutDashboard,
  Link2,
  PanelLeftClose,
  PanelLeftOpen,
  ScrollText,
  Settings,
  Share2,
  ShieldCheck,
  Trash2,
  User,
  X,
  type LucideIcon,
} from 'lucide-react';

import { Brand } from '@/components/common/Brand';
import { APP_ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/store/authStore';
import { useUiStore } from '@/store/uiStore';
import { cn } from '@/utils/cn';
import { isAdminRole } from '@/utils/role';

interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
}

const MAIN_NAV: readonly NavItem[] = [
  { label: 'Dashboard', to: APP_ROUTES.dashboard, icon: LayoutDashboard },
  { label: 'My Files', to: APP_ROUTES.files, icon: Files },
  { label: 'Folders', to: APP_ROUTES.folders, icon: FolderOpen },
  { label: 'Shared', to: APP_ROUTES.shared, icon: Share2 },
  { label: 'My Shares', to: APP_ROUTES.myShares, icon: Link2 },
  { label: 'Trash', to: APP_ROUTES.trash, icon: Trash2 },
];

const INSIGHTS_NAV: readonly NavItem[] = [
  { label: 'Analytics', to: APP_ROUTES.analytics, icon: BarChart3 },
  { label: 'Audit logs', to: APP_ROUTES.auditLogs, icon: ScrollText },
];

const ACCOUNT_NAV: readonly NavItem[] = [
  { label: 'Notifications', to: APP_ROUTES.notifications, icon: Bell },
  { label: 'Security', to: APP_ROUTES.security, icon: ShieldCheck },
  { label: 'Profile', to: APP_ROUTES.profile, icon: User },
  { label: 'Settings', to: APP_ROUTES.settings, icon: Settings },
  { label: 'About', to: APP_ROUTES.about, icon: CircleHelp },
];

const ADMIN_NAV: readonly NavItem[] = [
  { label: 'Admin', to: APP_ROUTES.admin, icon: ShieldCheck },
];

interface NavSectionProps {
  label?: string;
  items: readonly NavItem[];
  collapsed: boolean;
}

function NavSection({ label, items, collapsed }: NavSectionProps) {
  return (
    <div>
      {label && !collapsed && (
        <p className="mb-2 px-3 text-[11px] font-semibold tracking-wider text-gray-400 uppercase dark:text-gray-500">
          {label}
        </p>
      )}
      <ul className="space-y-1">
        {items.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end={item.to === APP_ROUTES.dashboard}
              title={collapsed ? item.label : undefined}
              className={({ isActive }) =>
                cn(
                  'group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-brand-600 shadow-brand-600/25 dark:bg-brand-500 text-white shadow-sm'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800/70 dark:hover:text-white',
                  collapsed && 'justify-center px-0',
                )
              }
            >
              <item.icon className="h-5 w-5 shrink-0" />
              {!collapsed && <span className="truncate">{item.label}</span>}
            </NavLink>
          </li>
        ))}
      </ul>
    </div>
  );
}

interface SidebarContentProps {
  collapsed?: boolean;
}

function SidebarContent({ collapsed = false }: SidebarContentProps) {
  const role = useAuthStore((state) => state.user?.role);
  const isAdmin = isAdminRole(role);

  return (
    <div className="flex h-full flex-col">
      <div
        className={cn(
          'flex h-16 shrink-0 items-center border-b border-gray-200/70 px-5 dark:border-gray-800',
          collapsed && 'justify-center px-0',
        )}
      >
        <Brand compact={collapsed} />
      </div>

      <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-5">
        <NavSection items={MAIN_NAV} collapsed={collapsed} />
        <NavSection label="Insights" items={INSIGHTS_NAV} collapsed={collapsed} />
        <NavSection label="Account" items={ACCOUNT_NAV} collapsed={collapsed} />
        {isAdmin && (
          <NavSection label="Administration" items={ADMIN_NAV} collapsed={collapsed} />
        )}
      </nav>
    </div>
  );
}

export function Sidebar() {
  const collapsed = useUiStore((state) => state.sidebarCollapsed);
  const toggleCollapsed = useUiStore((state) => state.toggleSidebar);
  const mobileOpen = useUiStore((state) => state.mobileSidebarOpen);
  const closeMobile = useUiStore((state) => state.closeMobileSidebar);

  return (
    <>
      {/* Desktop sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 hidden border-r border-gray-200/70 bg-white/80 backdrop-blur-xl transition-[width] duration-300 lg:block dark:border-gray-800 dark:bg-gray-950/80',
          collapsed ? 'w-20' : 'w-64',
        )}
      >
        <SidebarContent collapsed={collapsed} />

        <button
          type="button"
          onClick={toggleCollapsed}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          className="hover:text-brand-600 dark:hover:text-brand-400 absolute top-[4.5rem] -right-3 grid h-7 w-7 place-items-center rounded-full border border-gray-200 bg-white text-gray-500 shadow-sm transition-colors dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
        >
          {collapsed ? (
            <PanelLeftOpen className="h-3.5 w-3.5" />
          ) : (
            <PanelLeftClose className="h-3.5 w-3.5" />
          )}
        </button>
      </aside>

      {/* Mobile drawer */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              className="fixed inset-0 z-50 bg-gray-950/50 backdrop-blur-sm lg:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={closeMobile}
              aria-hidden="true"
            />
            <motion.aside
              className="fixed inset-y-0 left-0 z-50 w-72 border-r border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-950"
              initial={{ x: -300 }}
              animate={{ x: 0 }}
              exit={{ x: -300 }}
              transition={{ type: 'spring', stiffness: 320, damping: 32 }}
            >
              <button
                type="button"
                onClick={closeMobile}
                aria-label="Close navigation menu"
                className="absolute top-4 right-3 z-10 grid h-8 w-8 place-items-center rounded-lg text-gray-500 transition-colors hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
              >
                <X className="h-5 w-5" />
              </button>
              <SidebarContent />
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
