import { useState } from 'react';
import { Eye, Link2, MousePointerClick, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';

import { PermissionBadge, ResourceBadge } from '@/components/shares/ShareBadges';
import { Button } from '@/components/ui/Button';
import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { SHARE_EXPIRY_OPTIONS } from '@/constants/files';
import {
  useRevokeShareMutation,
  useShareAnalyticsQuery,
  useUpdateShareMutation,
} from '@/hooks/useShare';
import type { SharePermission, ShareRecord, UpdateShareRequest } from '@/types';
import { copyToClipboard } from '@/utils/download';
import { buildShareUrl } from '@/utils/file';
import { formatRelativeTime, toLocalDateTimeIso } from '@/utils/format';

export interface ShareSettingsDialogProps {
  share: ShareRecord | null;
  open: boolean;
  onClose: () => void;
}

const selectClasses =
  'mt-1.5 h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm text-gray-900 shadow-sm transition-colors focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25 focus:outline-none dark:border-gray-700 dark:bg-gray-950 dark:text-white';

/**
 * Maps an existing expiry timestamp back to one of the preset options
 * ('' = never, 'custom' = an off-preset date kept unchanged).
 */
function expiryToPreset(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const days = Math.round((new Date(iso).getTime() - Date.now()) / 86_400_000);
  const match = SHARE_EXPIRY_OPTIONS.find(
    (option) => option.value !== null && Number(option.value) === days,
  );
  return match?.value ?? 'custom';
}

/** Owner link-management dialog: analytics + permission/expiry/password + revoke. */
export function ShareSettingsDialog({ share, open, onClose }: ShareSettingsDialogProps) {
  const updateMutation = useUpdateShareMutation();
  const revokeMutation = useRevokeShareMutation();
  const analyticsQuery = useShareAnalyticsQuery(share?.id ?? null);

  // ── Form state ────────────────────────────────────────────────────────────
  const [permission, setPermission] = useState<SharePermission>('VIEW');
  const [expiryPreset, setExpiryPreset] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [removePassword, setRemovePassword] = useState(false);
  const [revokeOpen, setRevokeOpen] = useState(false);

  // Reset the form on every open so it always reflects the current share
  // record (adjust state during render instead of in an effect).
  const [prevOpen, setPrevOpen] = useState(false);
  const shouldReset = open && !prevOpen;
  if (shouldReset) {
    setPrevOpen(true);
    setPermission(share?.permission ?? 'VIEW');
    setExpiryPreset(share ? expiryToPreset(share.expiryDate) : '');
    setNewPassword('');
    setRemovePassword(false);
    setRevokeOpen(false);
  } else if (!open && prevOpen) {
    setPrevOpen(false);
  }

  if (!share) {
    return null;
  }

  const analytics = analyticsQuery.data;

  // ── Actions ───────────────────────────────────────────────────────────────

  const handleCopyLink = async () => {
    const ok = await copyToClipboard(buildShareUrl(share.shareToken));
    if (ok) {
      toast.success('Link copied to clipboard');
    } else {
      toast.error('Could not copy the link');
    }
  };

  const handleSave = async () => {
    const body: UpdateShareRequest = {
      permission,
      // Never expires → clear; custom → leave untouched; preset → new date.
      ...(expiryPreset === ''
        ? { clearExpiry: true }
        : expiryPreset === 'custom'
          ? {}
          : { expiryDate: toLocalDateTimeIso(new Date(Date.now() + Number(expiryPreset) * 86_400_000)) }),
      ...(removePassword
        ? { clearPassword: true }
        : newPassword.trim()
          ? { password: newPassword.trim() }
          : {}),
    };
    try {
      await updateMutation.mutateAsync({ id: share.id, body });
      onClose();
    } catch {
      // Toast handled by the mutation.
    }
  };

  const handleRevoke = async () => {
    try {
      await revokeMutation.mutateAsync(share.id);
      setRevokeOpen(false);
      onClose();
    } catch {
      // Toast handled by the mutation.
    }
  };

  const saving = updateMutation.isPending;

  return (
    <>
      <Modal
        open={open}
        onClose={onClose}
        title="Link settings"
        description="Manage access, protection and analytics for this share link."
        size="md"
      >
        <div className="space-y-5">
          {/* Resource + link */}
          <div className="flex items-center justify-between gap-3 rounded-xl bg-gray-50 p-3 dark:bg-gray-950">
            <ResourceBadge share={share} />
            <PermissionBadge permission={share.permission} />
          </div>

          <div>
            <p className="mb-1.5 text-sm font-medium text-gray-700 dark:text-gray-200">Link</p>
            <div className="flex items-center gap-2">
              <p className="min-w-0 flex-1 truncate rounded-lg border border-gray-200 bg-white px-3 py-2 font-mono text-xs text-gray-600 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-300">
                {buildShareUrl(share.shareToken)}
              </p>
              <Button variant="outline" size="sm" onClick={() => void handleCopyLink()}>
                Copy
              </Button>
            </div>
          </div>

          {/* Analytics */}
          <div className="grid grid-cols-3 gap-2">
            <div className="flex items-center gap-2 rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
              <Eye className="h-4 w-4 shrink-0 text-brand-500" />
              <div>
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Views</p>
                <p className="text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                  {analytics?.viewCount ?? share.viewCount ?? 0}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
              <MousePointerClick className="h-4 w-4 shrink-0 text-brand-500" />
              <div>
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Downloads</p>
                <p className="text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                  {analytics?.downloadCount ?? share.downloadCount ?? 0}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
              <Link2 className="h-4 w-4 shrink-0 text-brand-500" />
              <div>
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Last access</p>
                <p className="text-sm font-semibold text-gray-900 dark:text-white">
                  {analytics?.lastAccessedAt || share.lastAccessedAt
                    ? formatRelativeTime(analytics?.lastAccessedAt ?? share.lastAccessedAt ?? '')
                    : '—'}
                </p>
              </div>
            </div>
          </div>

          {/* Settings */}
          <div className="grid grid-cols-2 gap-3">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-200">
              Permission
              <select
                value={permission}
                onChange={(event) => setPermission(event.target.value as SharePermission)}
                className={selectClasses}
              >
                <option value="VIEW">Can view</option>
                <option value="DOWNLOAD">Download only</option>
                <option value="EDIT">Can edit</option>
              </select>
            </label>

            <label className="block text-sm font-medium text-gray-700 dark:text-gray-200">
              Expires
              <select
                value={expiryPreset}
                onChange={(event) => setExpiryPreset(event.target.value)}
                className={selectClasses}
              >
                {SHARE_EXPIRY_OPTIONS.map((option) => (
                  <option key={option.label} value={option.value ?? ''}>
                    {option.label}
                  </option>
                ))}
                {expiryPreset === 'custom' && (
                  <option value="custom" disabled>
                    Keep custom date
                  </option>
                )}
              </select>
            </label>
          </div>

          <div className="space-y-2.5">
            <Input
              label={share.hasPassword ? 'New password (leave blank to keep)' : 'Set a password (optional)'}
              type="password"
              value={newPassword}
              onChange={(event) => {
                setNewPassword(event.target.value);
                if (event.target.value) {
                  setRemovePassword(false);
                }
              }}
              placeholder={share.hasPassword ? 'Enter a new password' : 'Protect the link'}
            />
            {share.hasPassword && (
              <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-600 dark:text-gray-300">
                <input
                  type="checkbox"
                  checked={removePassword}
                  onChange={(event) => {
                    setRemovePassword(event.target.checked);
                    if (event.target.checked) {
                      setNewPassword('');
                    }
                  }}
                  className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-950"
                />
                Remove password protection
              </label>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
            <Button
              variant="ghost"
              size="sm"
              disabled={saving}
              onClick={() => setRevokeOpen(true)}
              className="text-rose-600 hover:bg-rose-50 hover:text-rose-700 dark:text-rose-400 dark:hover:bg-rose-500/10"
              leftIcon={<Trash2 className="h-3.5 w-3.5" />}
            >
              Revoke link
            </Button>
            <div className="flex items-center gap-2">
              <Button variant="outline" onClick={onClose} disabled={saving}>
                Cancel
              </Button>
              <Button variant="primary" onClick={() => void handleSave()} isLoading={saving}>
                Save changes
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      <ConfirmationDialog
        open={revokeOpen}
        onClose={() => setRevokeOpen(false)}
        onConfirm={() => void handleRevoke()}
        title="Revoke this link?"
        description={`“${share.resourceName ?? 'This share'}” will stop being accessible through the link. Existing access in CloudNest is unaffected.`}
        confirmLabel="Revoke link"
        isLoading={revokeMutation.isPending}
      />
    </>
  );
}
