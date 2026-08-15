import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { adminService } from '@/services/admin.service';
import { getErrorMessage } from '@/utils/error';

// ── Query keys ─────────────────────────────────────────────────────────────
export const ADMIN_HEALTH_KEY = ['admin', 'system', 'health'] as const;
export const ADMIN_USER_SUMMARY_KEY = ['admin', 'users', 'summary'] as const;
export const ADMIN_STORAGE_KEY = ['admin', 'storage', 'overview'] as const;
export const ADMIN_MINIO_KEY = ['admin', 'minio', 'status'] as const;
export const ADMIN_SECURITY_KEY = ['admin', 'security', 'overview'] as const;

export const ADMIN_USERS_KEY = (page: number, size: number, query: string) =>
  ['admin', 'users', { page, size, query: query || 'all' }] as const;

export const ADMIN_AUDIT_KEY = (
  page: number,
  size: number,
  action: string | undefined,
  userId: number | undefined,
) => ['admin', 'audit', { page, size, action: action ?? 'all', userId: userId ?? 'all' }] as const;

export const ADMIN_LOGIN_HISTORY_KEY = (page: number, size: number) =>
  ['admin', 'login-history', page, size] as const;

export const ADMIN_SECURITY_LOGS_KEY = (page: number, size: number) =>
  ['admin', 'security-logs', page, size] as const;

// ── Queries ────────────────────────────────────────────────────────────────
export function useAdminSystemHealth() {
  return useQuery({
    queryKey: ADMIN_HEALTH_KEY,
    queryFn: async () => {
      const { data } = await adminService.getSystemHealth();
      return data.data;
    },
  });
}

export function useAdminUserSummary() {
  return useQuery({
    queryKey: ADMIN_USER_SUMMARY_KEY,
    queryFn: async () => {
      const { data } = await adminService.getUserSummary();
      return data.data;
    },
  });
}

export function useAdminUsers(page: number, size: number, query: string) {
  return useQuery({
    queryKey: ADMIN_USERS_KEY(page, size, query),
    queryFn: async () => {
      const { data } = await adminService.getUsers({ page, size, query: query || undefined });
      return data.data;
    },
  });
}

export function useAdminStorageOverview() {
  return useQuery({
    queryKey: ADMIN_STORAGE_KEY,
    queryFn: async () => {
      const { data } = await adminService.getStorageOverview();
      return data.data;
    },
  });
}

export function useAdminAuditLogs(
  page: number,
  size: number,
  action: string | undefined,
  userId: number | undefined,
) {
  return useQuery({
    queryKey: ADMIN_AUDIT_KEY(page, size, action, userId),
    queryFn: async () => {
      const { data } = await adminService.getAuditLogs({
        page,
        size,
        action: action || undefined,
        userId,
      });
      return data.data;
    },
  });
}

export function useAdminMinioStatus() {
  return useQuery({
    queryKey: ADMIN_MINIO_KEY,
    queryFn: async () => {
      const { data } = await adminService.getMinioStatus();
      return data.data;
    },
  });
}

export function useAdminSecurityOverview() {
  return useQuery({
    queryKey: ADMIN_SECURITY_KEY,
    queryFn: async () => {
      const { data } = await adminService.getSecurityOverview();
      return data.data;
    },
  });
}

export function useAdminLoginHistory(page = 0, size = 20) {
  return useQuery({
    queryKey: ADMIN_LOGIN_HISTORY_KEY(page, size),
    queryFn: async () => {
      const { data } = await adminService.getLoginHistory(page, size);
      return data.data;
    },
  });
}

export function useAdminSecurityLogs(page = 0, size = 20) {
  return useQuery({
    queryKey: ADMIN_SECURITY_LOGS_KEY(page, size),
    queryFn: async () => {
      const { data } = await adminService.getSecurityLogs(page, size);
      return data.data;
    },
  });
}

// ── Mutations ──────────────────────────────────────────────────────────────
/** Enable/disable + role mutations for the admin users view. */
export function useAdminUserMutations() {
  const queryClient = useQueryClient();

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    void queryClient.invalidateQueries({ queryKey: ADMIN_USER_SUMMARY_KEY });
  };

  const setEnabled = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      adminService.setUserEnabled(id, enabled),
    onSuccess: (_data, variables) => {
      toast.success(variables.enabled ? 'User enabled' : 'User disabled');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to update the user.')),
  });

  const setRole = useMutation({
    mutationFn: ({ id, role }: { id: number; role: 'ROLE_ADMIN' | 'ROLE_USER' }) =>
      adminService.setUserRole(id, role),
    onSuccess: (_data, variables) => {
      toast.success(variables.role === 'ROLE_ADMIN' ? 'User promoted to admin' : 'Admin rights removed');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to change the role.')),
  });

  return { setEnabled, setRole };
}
