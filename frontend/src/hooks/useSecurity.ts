import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { authService } from '@/services/auth.service';
import { getErrorMessage } from '@/utils/error';

export const SECURITY_OVERVIEW_KEY = ['security', 'overview'] as const;
export const SESSIONS_KEY = ['security', 'sessions'] as const;
export const TRUSTED_DEVICES_KEY = ['security', 'trusted-devices'] as const;

/** Aggregated security posture. */
export function useSecurityOverview() {
  return useQuery({
    queryKey: SECURITY_OVERVIEW_KEY,
    queryFn: async () => {
      const { data } = await authService.getSecurityOverview();
      return data.data;
    },
  });
}

/** Active sessions (devices). */
export function useSessions() {
  return useQuery({
    queryKey: SESSIONS_KEY,
    queryFn: async () => {
      const { data } = await authService.getSessions();
      return data.data;
    },
  });
}

/** Trusted devices. */
export function useTrustedDevices() {
  return useQuery({
    queryKey: TRUSTED_DEVICES_KEY,
    queryFn: async () => {
      const { data } = await authService.getTrustedDevices();
      return data.data;
    },
  });
}

/** Paginated sign-in history. */
export function useLoginHistory(page = 0, size = 20) {
  return useQuery({
    queryKey: ['security', 'login-history', page, size],
    queryFn: async () => {
      const { data } = await authService.getLoginHistory(page, size);
      return data.data;
    },
  });
}

/** Paginated security log. */
export function useSecurityLogs(page = 0, size = 20) {
  return useQuery({
    queryKey: ['security', 'security-logs', page, size],
    queryFn: async () => {
      const { data } = await authService.getSecurityLogs(page, size);
      return data.data;
    },
  });
}

/** Session / device management mutations. */
export function useSecurityMutations() {
  const queryClient = useQueryClient();

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: SESSIONS_KEY });
    void queryClient.invalidateQueries({ queryKey: TRUSTED_DEVICES_KEY });
    void queryClient.invalidateQueries({ queryKey: SECURITY_OVERVIEW_KEY });
    void queryClient.invalidateQueries({ queryKey: ['security', 'login-history'] });
    void queryClient.invalidateQueries({ queryKey: ['security', 'security-logs'] });
  };

  const endSession = useMutation({
    mutationFn: (sessionId: string) => authService.endSession(sessionId),
    onSuccess: () => {
      toast.success('Session ended.');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to end the session.')),
  });

  const logoutAll = useMutation({
    mutationFn: () => authService.logoutAll(),
    onSuccess: () => {
      toast.success('Logged out from all other devices.');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to log out all devices.')),
  });

  const removeTrustedDevice = useMutation({
    mutationFn: (id: number) => authService.removeTrustedDevice(id),
    onSuccess: () => {
      toast.success('Trusted device removed — OTP will be required again.');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to remove the device.')),
  });

  return { endSession, logoutAll, removeTrustedDevice };
}
