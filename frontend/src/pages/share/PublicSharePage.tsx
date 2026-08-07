import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { motion } from 'framer-motion';
import { Clock3, LogIn, Unlink } from 'lucide-react';
import { toast } from 'react-toastify';

import { Brand } from '@/components/common/Brand';
import { PublicShareCard } from '@/components/share-public/PublicShareCard';
import { SharePasswordGate } from '@/components/share-public/SharePasswordGate';
import { Button } from '@/components/ui/Button';
import { APP_ROUTES } from '@/constants/routes';
import { usePublicShareQuery } from '@/hooks/useShare';
import { shareService } from '@/services/share.service';
import { selectIsAuthenticated, useAuthStore } from '@/store/authStore';
import { blobDownload, copyToClipboard } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';
import { buildShareUrl } from '@/utils/file';

type PublicShareErrorKind = 'not-found' | 'expired' | 'other';

function errorKind(error: unknown): PublicShareErrorKind {
  if (isAxiosError(error)) {
    if (error.response?.status === 404) return 'not-found';
    if (error.response?.status === 410) return 'expired';
  }
  return 'other';
}

/** Shared, unauthenticated page for opening a CloudNest share link. */
export function PublicSharePage() {
  const { token } = useParams<{ token: string }>();
  const isAuthenticated = useAuthStore(selectIsAuthenticated);

  const query = usePublicShareQuery(token);
  const share = query.data;

  // Password gate state.
  const [verifiedPassword, setVerifiedPassword] = useState<string | null>(null);
  const [gateError, setGateError] = useState<string | undefined>();
  const [verifying, setVerifying] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const unlocked = !share?.hasPassword || verifiedPassword !== null;

  // ── Actions ───────────────────────────────────────────────────────────────

  const handleUnlock = async (password: string) => {
    if (!token || !password.trim()) {
      setGateError('Enter the share password.');
      return;
    }
    setVerifying(true);
    setGateError(undefined);
    try {
      await shareService.verifySharePassword(token, { password: password.trim() });
      setVerifiedPassword(password.trim());
    } catch (error) {
      setGateError(getErrorMessage(error, 'The share password is incorrect.'));
    } finally {
      setVerifying(false);
    }
  };

  const handleDownload = async () => {
    if (!token || !share) {
      return;
    }
    setDownloading(true);
    try {
      const { data } = await shareService.downloadPublicShare(
        token,
        verifiedPassword ?? undefined,
      );
      blobDownload(data, share.resourceName ?? 'shared-file');
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        // The password is no longer accepted — send the visitor back to the gate.
        setVerifiedPassword(null);
        setGateError('This link now requires a different password.');
      } else {
        toast.error(getErrorMessage(error, 'The download could not be started.'));
      }
    } finally {
      setDownloading(false);
    }
  };

  const handleCopyLink = async () => {
    if (!token) {
      return;
    }
    const ok = await copyToClipboard(buildShareUrl(token));
    if (ok) {
      toast.success('Link copied to clipboard');
    } else {
      toast.error('Could not copy the link');
    }
  };

  // ── Render helpers ────────────────────────────────────────────────────────

  const homeHref = isAuthenticated ? APP_ROUTES.dashboard : APP_ROUTES.login;

  const renderError = (kind: PublicShareErrorKind) => {
    const expired = kind === 'expired';
    return (
      <div className="flex flex-col items-center text-center">
        <div
          className={
            expired
              ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 mb-4 grid h-14 w-14 place-items-center rounded-2xl'
              : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 mb-4 grid h-14 w-14 place-items-center rounded-2xl'
          }
        >
          {expired ? <Clock3 className="h-7 w-7" /> : <Unlink className="h-7 w-7" />}
        </div>
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
          {expired ? 'This link has expired' : 'This link is not available'}
        </h2>
        <p className="mt-1.5 max-w-sm text-sm text-gray-500 dark:text-gray-400">
          {expired
            ? 'The owner set an expiration date for this link, and it has passed. Ask the owner for a new link.'
            : 'The link may have been removed by its owner, or the address may be wrong.'}
        </p>
        <Link to={homeHref} className="mt-6">
          <Button variant="outline" leftIcon={<LogIn className="h-4 w-4" />}>
            {isAuthenticated ? 'Go to My Files' : 'Sign in to CloudNest'}
          </Button>
        </Link>
      </div>
    );
  };

  return (
    <div className="bg-brand-50/60 dark:bg-gray-950 flex min-h-dvh flex-col">
      {/* Top bar */}
      <header className="flex items-center justify-between gap-4 px-5 py-5 sm:px-8">
        <Brand />
        <Link
          to={homeHref}
          className="text-sm font-medium text-gray-500 transition-colors hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
        >
          {isAuthenticated ? 'Go to My Files' : 'Sign in'}
        </Link>
      </header>

      {/* Card */}
      <main className="flex flex-1 items-center justify-center px-4 pb-16">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
          className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-6 shadow-xl shadow-gray-900/[0.06] sm:p-8 dark:border-gray-800 dark:bg-gray-900 dark:shadow-black/20"
        >
          {query.isLoading ? (
            <div className="space-y-5">
              <div className="mx-auto grid h-16 w-16 animate-pulse place-items-center rounded-2xl bg-gray-100 dark:bg-gray-800" />
              <div className="mx-auto h-4 w-2/3 animate-pulse rounded-full bg-gray-100 dark:bg-gray-800" />
              <div className="mx-auto h-3 w-1/3 animate-pulse rounded-full bg-gray-100 dark:bg-gray-800" />
              <div className="grid grid-cols-4 gap-2">
                {[0, 1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="h-16 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800"
                  />
                ))}
              </div>
            </div>
          ) : query.isError ? (
            renderError(errorKind(query.error))
          ) : !share ? (
            renderError('not-found')
          ) : !unlocked ? (
            <SharePasswordGate
              resourceName={share.resourceName}
              submitting={verifying}
              error={gateError}
              onSubmit={(password) => void handleUnlock(password)}
              onInputChange={() => setGateError(undefined)}
            />
          ) : (
            <PublicShareCard
              share={share}
              password={verifiedPassword}
              downloading={downloading}
              onDownload={() => void handleDownload()}
              onCopyLink={() => void handleCopyLink()}
            />
          )}
        </motion.div>
      </main>

      <footer className="pb-6 text-center text-xs text-gray-400 dark:text-gray-600">
        Powered by CloudNest — secure file sharing
      </footer>
    </div>
  );
}
