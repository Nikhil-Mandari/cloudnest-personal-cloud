import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import {
  Check,
  Copy,
  Fingerprint,
  KeyRound,
  RefreshCcw,
  ShieldCheck,
  ShieldOff,
  Smartphone,
  Trash2,
} from 'lucide-react';

import { EmptyState } from '@/components/common/EmptyState';
import { Loader } from '@/components/common/Loader';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { useMfaMutations, usePasskeys, useTwoFactorStatus } from '@/hooks/useSecurity';
import type { PasskeyCredentialInfo, TwoFactorSetup } from '@/types';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/format';

// ═════════════════════════════════════════════════════════════════════════
// Two-factor authentication (TOTP)
// ═════════════════════════════════════════════════════════════════════════

export function TwoFactorPanel() {
  const status = useTwoFactorStatus();
  const { setupTwoFactor, enableTwoFactor, disableTwoFactor, regenerateBackupCodes } =
    useMfaMutations();

  const [setupOpen, setSetupOpen] = useState(false);
  const [setup, setSetup] = useState<TwoFactorSetup | null>(null);
  const [code, setCode] = useState('');
  const [codesOpen, setCodesOpen] = useState(false);
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null);
  const [disableOpen, setDisableOpen] = useState(false);
  const [verification, setVerification] = useState('');
  const [copied, setCopied] = useState(false);

  /** Shows a freshly generated backup-code set in its own modal. */
  const showBackupCodes = (codes: string[]) => {
    setBackupCodes(codes);
    setCodesOpen(true);
  };

  const enabled = Boolean(status.data?.enabled);
  const remaining = status.data?.backupCodesRemaining ?? 0;

  const openSetup = () => {
    setSetup(null);
    setCode('');
    setSetupOpen(true);
    setupTwoFactor.mutate(undefined, {
      onSuccess: ({ data }) => setSetup(data.data),
    });
  };

  const closeSetup = () => {
    setSetupOpen(false);
    setSetup(null);
    setCode('');
  };

  const submitEnable = () => {
    if (code.trim().length < 6) {
      return;
    }
    enableTwoFactor.mutate(code.trim(), {
      onSuccess: ({ data }) => {
        setCode('');
        closeSetup();
        showBackupCodes(data.data.backupCodes);
      },
    });
  };

  const copySecret = async () => {
    if (!setup) {
      return;
    }
    await navigator.clipboard.writeText(setup.secret).catch(() => undefined);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  const copyBackupCodes = async () => {
    if (!backupCodes) {
      return;
    }
    await navigator.clipboard.writeText(backupCodes.join('\n')).catch(() => undefined);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  return (
    <Card>
      <CardHeader
        title="Two-factor authentication"
        description="An extra code from your authenticator app when you sign in."
        action={
          enabled ? (
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<RefreshCcw className="h-3.5 w-3.5" />}
                onClick={() => {
                  if (window.confirm('Regenerate backup codes? Your existing unused codes will stop working.')) {
                    regenerateBackupCodes.mutate(undefined, {
                      onSuccess: ({ data }) => showBackupCodes(data.data.backupCodes),
                    });
                  }
                }}
              >
                New backup codes
              </Button>
              <Button
                variant="danger"
                size="sm"
                leftIcon={<ShieldOff className="h-3.5 w-3.5" />}
                onClick={() => {
                  setVerification('');
                  setDisableOpen(true);
                }}
              >
                Turn off
              </Button>
            </div>
          ) : (
            <Button size="sm" leftIcon={<ShieldCheck className="h-3.5 w-3.5" />} onClick={openSetup}>
              Set up
            </Button>
          )
        }
      />
      <CardBody>
        {status.isLoading ? (
          <Loader className="py-8" />
        ) : enabled ? (
          <div className="flex flex-wrap items-center gap-4">
            <div className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 grid h-12 w-12 shrink-0 place-items-center rounded-2xl">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-gray-900 dark:text-white">2FA is on</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Sign-ins require a code from Google Authenticator, Microsoft Authenticator or Authy.
              </p>
            </div>
            <span
              className={cn(
                'rounded-full px-2.5 py-1 text-xs font-medium',
                remaining === 0
                  ? 'bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300'
                  : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300',
              )}
            >
              {remaining === 0
                ? 'No backup codes left — generate new ones'
                : `${remaining} backup code${remaining === 1 ? '' : 's'} remaining`}
            </span>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-4">
            <div className="bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400 grid h-12 w-12 shrink-0 place-items-center rounded-2xl">
              <Smartphone className="h-6 w-6" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-gray-900 dark:text-white">2FA is off</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Turn it on to add a second layer of protection to your account.
              </p>
            </div>
          </div>
        )}
      </CardBody>

      {/* Setup / enable modal */}
      <Modal
        open={setupOpen}
        onClose={closeSetup}
        title="Set up two-factor authentication"
        description="Scan the QR code with your authenticator app, then enter the code it shows."
        size="md"
      >
        {setupTwoFactor.isPending && !setup ? (
          <Loader className="py-12" />
        ) : setup ? (
          // QR + code input
          <div className="space-y-5">
              <div className="flex flex-col items-center gap-4 sm:flex-row">
                <div className="shrink-0 rounded-2xl border border-gray-200 bg-white p-3 dark:border-gray-700">
                  <QRCodeSVG
                    value={setup.otpauthUri}
                    size={164}
                    level="M"
                    bgColor="#ffffff"
                    fgColor="#1f2937"
                  />
                </div>
                <div className="min-w-0 flex-1 space-y-3 text-sm">
                  <div>
                    <p className="text-xs text-gray-400 uppercase">Account</p>
                    <p className="font-medium text-gray-900 dark:text-white">{setup.accountName}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400 uppercase">Issuer</p>
                    <p className="font-medium text-gray-900 dark:text-white">{setup.issuer}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400 uppercase">Secret key</p>
                    <div className="flex items-center gap-2">
                      <code className="min-w-0 flex-1 truncate rounded-lg bg-gray-100 px-2.5 py-1.5 font-mono text-xs text-gray-700 dark:bg-gray-800 dark:text-gray-200">
                        {setup.secret}
                      </code>
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                        onClick={() => void copySecret()}
                      >
                        {copied ? 'Copied' : 'Copy'}
                      </Button>
                    </div>
                  </div>
                  <p className="text-xs text-gray-400 dark:text-gray-500">
                    Can&apos;t scan the QR? Enter the secret manually — it updates every {setup.periodSeconds}s.
                  </p>
                </div>
              </div>

              <Input
                label="Authenticator code"
                placeholder="000000"
                inputMode="numeric"
                maxLength={6}
                value={code}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
                error={
                  enableTwoFactor.isError ? 'That code did not work. Check the app and try again.' : undefined
                }
              />

              <div className="flex justify-end gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
                <Button variant="outline" onClick={closeSetup}>
                  Cancel
                </Button>
                <Button
                  isLoading={enableTwoFactor.isPending}
                  disabled={code.trim().length < 6}
                  onClick={submitEnable}
                >
                  Enable 2FA
                </Button>
              </div>
          </div>
        ) : null}
      </Modal>

      {/* Backup codes (shown exactly once after enable / regenerate) */}
      <Modal
        open={codesOpen && backupCodes !== null}
        onClose={() => setCodesOpen(false)}
        title="Save your backup codes"
        description="Each code works only once. You won't see them again — store them somewhere safe."
        size="md"
      >
        {backupCodes && (
          <div className="space-y-4">
            <div className="flex items-start gap-3 rounded-xl border border-amber-300/60 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300">
              <KeyRound className="mt-0.5 h-5 w-5 shrink-0" />
              <p>
                <span className="font-semibold">Save these backup codes.</span> If you lose your
                authenticator app, these are the only way back into your account.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-2">
              {backupCodes.map((codeItem) => (
                <code
                  key={codeItem}
                  className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-center font-mono text-sm font-semibold tracking-wide text-gray-800 dark:border-gray-700 dark:bg-gray-950 dark:text-gray-100"
                >
                  {codeItem}
                </code>
              ))}
            </div>

            <Button
              variant="outline"
              size="sm"
              leftIcon={copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
              onClick={() => void copyBackupCodes()}
            >
              {copied ? 'Copied' : 'Copy all'}
            </Button>

            <div className="flex justify-end gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
              <Button
                onClick={() => {
                  setBackupCodes(null);
                  setCodesOpen(false);
                }}
              >
                I&apos;ve saved my codes
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Disable modal */}
      <Modal
        open={disableOpen}
        onClose={() => setDisableOpen(false)}
        title="Turn off two-factor authentication?"
        description="Confirm with your authenticator code, an unused backup code, or your account password."
        size="sm"
      >
        <div className="space-y-4">
          <Input
            label="Verification"
            placeholder="Code or password"
            value={verification}
            onChange={(event) => setVerification(event.target.value)}
            error={
              disableTwoFactor.isError
                ? 'Verification failed. Check your input and try again.'
                : undefined
            }
          />
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setDisableOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              isLoading={disableTwoFactor.isPending}
              disabled={verification.trim().length === 0}
              onClick={() =>
                disableTwoFactor.mutate(verification.trim(), {
                  onSuccess: () => {
                    setDisableOpen(false);
                    setVerification('');
                  },
                })
              }
            >
              Turn off 2FA
            </Button>
          </div>
        </div>
      </Modal>
    </Card>
  );
}

// ═════════════════════════════════════════════════════════════════════════
// Passkeys (WebAuthn)
// ═════════════════════════════════════════════════════════════════════════

const TRANSPORT_LABELS: Record<string, string> = {
  internal: 'Platform · biometric',
  usb: 'USB security key',
  nfc: 'NFC',
  ble: 'Bluetooth',
  hybrid: 'Phone / tablet',
};

function transportLabel(transport: string): string {
  return TRANSPORT_LABELS[transport] ?? transport;
}

export function PasskeysPanel() {
  const passkeys = usePasskeys();
  const { registerPasskey, removePasskey } = useMfaMutations();

  const supported =
    typeof window !== 'undefined' && 'PublicKeyCredential' in window;

  const register = () => {
    const nickname = window.prompt('Name this passkey (optional)', '')?.trim();
    registerPasskey.mutate(nickname || undefined);
  };

  const remove = (credential: PasskeyCredentialInfo) => {
    const label = credential.nickname ?? 'This passkey';
    if (window.confirm(`Remove "${label}"? You will need another way to sign in from its device.`)) {
      removePasskey.mutate(credential.id);
    }
  };

  return (
    <Card>
      <CardHeader
        title="Passkeys"
        description="Sign in with Face ID, Touch ID, Windows Hello or a security key."
        action={
          supported ? (
            <Button
              size="sm"
              leftIcon={<Fingerprint className="h-3.5 w-3.5" />}
              isLoading={registerPasskey.isPending}
              onClick={register}
            >
              Add passkey
            </Button>
          ) : undefined
        }
      />
      <CardBody>
        {passkeys.isLoading ? (
          <Loader className="py-8" />
        ) : passkeys.data && passkeys.data.length > 0 ? (
          <div className="space-y-3">
            {passkeys.data.map((credential) => (
              <div
                key={credential.id}
                className="flex items-center gap-4 rounded-xl border border-gray-100 p-3.5 transition-colors hover:bg-gray-50 dark:border-gray-800 dark:hover:bg-gray-800/40"
              >
                <div className="bg-brand-500/10 text-brand-600 dark:bg-brand-400/10 dark:text-brand-300 grid h-11 w-11 shrink-0 place-items-center rounded-xl">
                  <Fingerprint className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-gray-900 dark:text-white">
                    {credential.nickname ?? 'Passkey'}
                  </p>
                  <div className="mt-1 flex flex-wrap items-center gap-1.5">
                    {credential.transports.length > 0 ? (
                      credential.transports.map((transport) => (
                        <span
                          key={transport}
                          className="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-300"
                        >
                          {transportLabel(transport)}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-gray-400">Biometric authenticator</span>
                    )}
                    <span className="text-xs text-gray-400">
                      Added {formatRelativeTime(credential.createdAt)}
                    </span>
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  leftIcon={<Trash2 className="h-4 w-4" />}
                  onClick={() => remove(credential)}
                >
                  Remove
                </Button>
              </div>
            ))}
          </div>
        ) : supported ? (
          <EmptyState
            icon={<Fingerprint className="h-7 w-7" />}
            title="No passkeys yet"
            description="Add one to sign in with your fingerprint, face or a security key."
          />
        ) : (
          <p className="rounded-xl bg-gray-100 px-4 py-3 text-sm text-gray-500 dark:bg-gray-800 dark:text-gray-400">
            Passkeys are not supported by this browser. Use a recent version of Chrome, Edge or Safari.
          </p>
        )}
      </CardBody>
    </Card>
  );
}
