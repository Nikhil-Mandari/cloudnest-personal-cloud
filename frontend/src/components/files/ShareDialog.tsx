import { useState } from 'react';
import { Eye, Lock, MousePointerClick, Link2 } from 'lucide-react';
import { toast } from 'react-toastify';

import { SHARE_EXPIRY_OPTIONS } from '@/constants/files';
import { EMAIL_PATTERN } from '@/constants/validation';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { useShareFileMutation } from '@/hooks/useShare';
import type { FileItem, SharePermission, ShareRecord } from '@/types';
import { copyToClipboard } from '@/utils/download';
import { buildShareUrl } from '@/utils/file';
import { toLocalDateTimeIso } from '@/utils/format';

export interface ShareDialogProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
}

const selectClasses =
  'mt-1.5 h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm text-gray-900 shadow-sm transition-colors focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25 focus:outline-none dark:border-gray-700 dark:bg-gray-950 dark:text-white';

/** Share dialog — recipient email + permission + expiry, then a copyable link. */
export function ShareDialog({ file, open, onClose }: ShareDialogProps) {
  const shareMutation = useShareFileMutation();

  const [email, setEmail] = useState('');
  const [permission, setPermission] = useState<SharePermission>('VIEW');
  const [expiryDays, setExpiryDays] = useState<string | null>(null);
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [createdShare, setCreatedShare] = useState<ShareRecord | null>(null);

  // Reset the form whenever the dialog opens for a (new) target file — adjust
  // state during render instead of in an effect (React recommended pattern).
  const [prevState, setPrevState] = useState<{ open: boolean; fileId: number | null }>({
    open: false,
    fileId: null,
  });
  const shouldReset = open && (prevState.open !== true || file?.id !== prevState.fileId);
  if (shouldReset) {
    setPrevState({ open: true, fileId: file?.id ?? null });
    setEmail('');
    setPermission('VIEW');
    setExpiryDays(null);
    setPassword('');
    setError(undefined);
    setCreatedShare(null);
  }

  const handleShare = async () => {
    if (!file) {
      return;
    }
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setError('Enter the email of the person you want to share with.');
      return;
    }
    if (!EMAIL_PATTERN.test(trimmedEmail)) {
      setError('Enter a valid email address.');
      return;
    }

    const expiryDate = expiryDays
      ? toLocalDateTimeIso(new Date(Date.now() + Number(expiryDays) * 86_400_000))
      : undefined;

    try {
      const { data } = await shareMutation.mutateAsync({
        fileId: file.id,
        sharedWithEmail: trimmedEmail,
        permission,
        expiryDate,
        password: password.trim() || undefined,
      });
      setCreatedShare(data.data);
      toast.success('Share link created');
    } catch {
      // Error toast is handled by the mutation.
    }
  };

  const handleCopyLink = async () => {
    if (!createdShare) {
      return;
    }
    const ok = await copyToClipboard(buildShareUrl(createdShare.shareToken));
    if (ok) {
      toast.success('Link copied to clipboard');
    } else {
      toast.error('Could not copy the link');
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Share file"
      description={file ? `Share "${file.originalFileName}"` : undefined}
      size="sm"
    >
      {createdShare ? (
        <div className="space-y-4">
          <div className="rounded-xl bg-emerald-500/10 p-4">
            <p className="text-sm font-medium text-emerald-700 dark:text-emerald-300">
              Share link ready
            </p>
            <p className="mt-1 text-xs break-all text-gray-500 dark:text-gray-400">
              {buildShareUrl(createdShare.shareToken)}
            </p>
            {createdShare.hasPassword && (
              <p className="mt-2 flex items-center gap-1.5 text-xs font-medium text-amber-700 dark:text-amber-300">
                <Lock className="h-3.5 w-3.5" /> Password protected — share the password separately
              </p>
            )}
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div className="flex items-center gap-2 rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
              <Eye className="h-4 w-4 shrink-0 text-brand-500" />
              <div>
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Views</p>
                <p className="text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                  {createdShare.viewCount ?? 0}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
              <MousePointerClick className="h-4 w-4 shrink-0 text-brand-500" />
              <div>
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Downloads</p>
                <p className="text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                  {createdShare.downloadCount ?? 0}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
              <Lock className="h-4 w-4 shrink-0 text-brand-500" />
              <div>
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Password</p>
                <p className="text-sm font-semibold text-gray-900 dark:text-white">
                  {createdShare.hasPassword ? 'On' : 'Off'}
                </p>
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-3">
            <Button variant="outline" onClick={onClose}>
              Done
            </Button>
            <Button
              variant="primary"
              onClick={() => void handleCopyLink()}
              leftIcon={<Link2 className="h-4 w-4" />}
            >
              Copy link
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <Input
            label="Share with"
            type="email"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value);
              setError(undefined);
            }}
            placeholder="teammate@example.com"
            hint="The recipient needs a CloudNest account."
            error={error}
          />

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
                value={expiryDays ?? ''}
                onChange={(event) => setExpiryDays(event.target.value || null)}
                className={selectClasses}
              >
                {SHARE_EXPIRY_OPTIONS.map((option) => (
                  <option key={option.label} value={option.value ?? ''}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <Input
            label="Password (optional)"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Protect the link with a password"
            hint="Recipients must enter this password to open or download the file."
          />

          <div className="flex justify-end gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
            <Button variant="outline" onClick={onClose} disabled={shareMutation.isPending}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={() => void handleShare()}
              isLoading={shareMutation.isPending}
            >
              Create link
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
}
