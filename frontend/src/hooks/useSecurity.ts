import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { authService } from '@/services/auth.service';
import { getErrorMessage } from '@/utils/error';

export const SECURITY_OVERVIEW_KEY = ['security', 'overview'] as const;
export const SESSIONS_KEY = ['security', 'sessions'] as const;
export const TRUSTED_DEVICES_KEY = ['security', 'trusted-devices'] as const;
export const TWO_FACTOR_KEY = ['security', 'two-factor'] as const;
export const PASSKEYS_KEY = ['security', 'passkeys'] as const;

import { isPasskeySupported } from '@/utils/passkeys';

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

/** Current 2FA state (enabled flag + unused backup codes). */
export function useTwoFactorStatus() {
  return useQuery({
    queryKey: TWO_FACTOR_KEY,
    queryFn: async () => {
      const { data } = await authService.getTwoFactorStatus();
      return data.data;
    },
  });
}

/** Registered passkeys, newest first. */
export function usePasskeys() {
  return useQuery({
    queryKey: PASSKEYS_KEY,
    queryFn: async () => {
      const { data } = await authService.listPasskeys();
      return data.data;
    },
  });
}

/** 2FA setup / enable / disable / backup-code + passkey mutations. */
export function useMfaMutations() {
  const queryClient = useQueryClient();

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: TWO_FACTOR_KEY });
    void queryClient.invalidateQueries({ queryKey: PASSKEYS_KEY });
    void queryClient.invalidateQueries({ queryKey: SECURITY_OVERVIEW_KEY });
  };

  const setup = useMutation({
    mutationFn: () => authService.twoFactorSetup(),
    onError: (error) => toast.error(getErrorMessage(error, 'Could not start 2FA setup.')),
  });

  const enable = useMutation({
    mutationFn: (code: string) => authService.enableTwoFactor({ code }),
    onSuccess: () => {
      toast.success('Two-factor authentication is now on.');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'That code did not work. Try again.')),
  });

  const disable = useMutation({
    mutationFn: (verification: string) => authService.disableTwoFactor({ verification }),
    onSuccess: () => {
      toast.success('Two-factor authentication is now off.');
      invalidate();
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, 'Could not disable 2FA. Check your verification input.')),
  });

  const regenerate = useMutation({
    mutationFn: () => authService.regenerateBackupCodes(),
    onSuccess: () => {
      toast.success('New backup codes generated.');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Could not regenerate backup codes.')),
  });

  const registerPasskey = useMutation({
    mutationFn: async (nickname?: string) => {
      const { data: startData } = await authService.passkeyRegisterStart();
      const started = startData.data;

      if (!isPasskeySupported()) {
        throw new Error('Passkeys are not supported by this browser. Use a recent Chrome, Edge or Safari.');
      }

      const credential = await navigator.credentials.create({
        publicKey: JSON.parse(started.optionsJson) as PublicKeyCredentialCreationOptions,
      });
      if (!credential) {
        throw new Error('Passkey registration was cancelled.');
      }

      const responseJson =
        'toJSON' in credential
          ? JSON.stringify((credential as { toJSON: () => unknown }).toJSON())
          : JSON.stringify(credential);

      const { data } = await authService.passkeyRegisterFinish({
        optionsJson: started.optionsJson,
        responseJson,
        nickname,
      });
      return data.data;
    },
    onSuccess: (credential) => {
      toast.success(
        credential.nickname
          ? `Passkey "${credential.nickname}" registered.`
          : 'Passkey registered — you can now sign in with it.',
      );
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Could not register the passkey.')),
  });

  const removePasskey = useMutation({
    mutationFn: (id: string) => authService.removePasskey(id),
    onSuccess: () => {
      toast.success('Passkey removed.');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Could not remove the passkey.')),
  });

  return {
    setupTwoFactor: setup,
    enableTwoFactor: enable,
    disableTwoFactor: disable,
    regenerateBackupCodes: regenerate,
    registerPasskey,
    removePasskey,
  };
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
