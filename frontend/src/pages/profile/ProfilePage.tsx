import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { motion } from 'framer-motion';
import {
  AtSign,
  BadgeCheck,
  CalendarDays,
  Camera,
  Files,
  FolderOpen,
  HardDrive,
  History,
  KeyRound,
  Lock,
  Mail,
  Save,
  ShieldCheck,
  Upload,
} from 'lucide-react';
import { toast } from 'react-toastify';

import { Avatar } from '@/components/common/Avatar';
import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { FileIcon } from '@/components/files/FileIcon';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import {
  EMAIL_PATTERN,
  NAME_MIN_LENGTH,
  PASSWORD_MIN_LENGTH,
  PASSWORD_PATTERN,
  PASSWORD_REQUIREMENTS_MESSAGE,
} from '@/constants/validation';
import { useProfileStats } from '@/hooks/useProfileStats';
import {
  useChangePasswordMutation,
  useProfileMutations,
  useProfileQuery,
} from '@/hooks/useProfile';
import { fileService } from '@/services/file.service';
import { cn } from '@/utils/cn';
import { formatFileDate, isAvatarFileId, isPdfFile } from '@/utils/file';
import { formatBytes, formatRelativeTime } from '@/utils/format';
import { isAdminRole } from '@/utils/role';

interface ProfileFormValues {
  displayName: string;
  email: string;
  phone: string;
  bio: string;
}

interface ChangePasswordFormValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const EMPTY_FORM: ProfileFormValues = { displayName: '', email: '', phone: '', bio: '' };

const EMPTY_PASSWORD_FORM: ChangePasswordFormValues = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

/** Maximum dimension (px) the avatar is downscaled to before upload. */
const AVATAR_MAX_DIMENSION = 256;

/**
 * Downscales + re-encodes an image to a small PNG so avatars stay lightweight.
 * Returns `null` when the browser cannot decode the file.
 */
async function resizeAvatarImage(file: File): Promise<File | null> {
  try {
    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, AVATAR_MAX_DIMENSION / Math.max(bitmap.width, bitmap.height));
    const width = Math.max(1, Math.round(bitmap.width * scale));
    const height = Math.max(1, Math.round(bitmap.height * scale));

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      bitmap.close();
      return null;
    }
    ctx.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();

    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/png'),
    );
    if (!blob) {
      return null;
    }
    return new File([blob], `avatar-${Date.now()}.png`, { type: 'image/png' });
  } catch {
    return null;
  }
}

/** Profile page: account summary, avatar, storage stats and editable profile. */
export function ProfilePage() {
  const { data: profile, isLoading, isError, refetch } = useProfileQuery();
  const { updateProfile } = useProfileMutations();
  const stats = useProfileStats();

  const avatarInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileFormValues>({ defaultValues: EMPTY_FORM });

  // Hydrate the form once the profile loads (or after a save).
  useEffect(() => {
    if (profile) {
      reset({
        displayName: profile.displayName ?? profile.username ?? '',
        email: profile.email ?? '',
        phone: profile.phone ?? '',
        bio: profile.bio ?? '',
      });
    }
  }, [profile, reset]);

  const onSubmit = (values: ProfileFormValues) => {
    // On success the profile cache is refreshed, which re-hydrates the form
    // from the server's response via the effect above.
    updateProfile.mutate(values);
  };

  /**
   * Uploads a chosen image as the user's avatar. CloudNest stores avatars
   * through the file-service: the image is uploaded as a regular file and its
   * numeric id is saved to `avatarUrl`. A previous avatar file (if any) is
   * permanently removed afterwards so re-uploads don't pile up.
   */
  const handleAvatarFile = async (file: File | undefined) => {
    if (!file || !profile) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      toast.error('Please choose an image file for your avatar.');
      return;
    }
    const resized = await resizeAvatarImage(file);
    if (!resized) {
      toast.error('Could not process that image — try a different one.');
      return;
    }
    const previousAvatarId = isAvatarFileId(profile.avatarUrl) ? Number(profile.avatarUrl) : null;
    try {
      const { data } = await fileService.uploadFile(resized);
      const newAvatarId = data.data.file?.id;
      if (newAvatarId === undefined) {
        toast.error('Could not upload your avatar. Please try again.');
        return;
      }
      await updateProfile.mutateAsync({ avatarUrl: String(newAvatarId) });
      toast.success('Avatar updated');
      // Best-effort cleanup of the superseded avatar file (removed from the
      // cloud + trash so re-uploads never accumulate).
      if (previousAvatarId !== null && previousAvatarId !== newAvatarId) {
        try {
          await fileService.deleteFile(previousAvatarId);
          await fileService.permanentlyDeleteFile(previousAvatarId);
        } catch {
          // Non-fatal — the old file simply stays in the account.
        }
      }
    } catch {
      toast.error('Failed to upload your avatar. Please try again.');
    }
  };

  const displayName = profile?.displayName ?? profile?.username ?? 'You';
  const storagePct = stats.storagePercent;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="h-9 w-48 animate-pulse rounded-full bg-gray-100 dark:bg-gray-800" />
        <div className="grid animate-pulse grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="h-80 rounded-2xl border border-gray-200/80 bg-white dark:border-gray-800 dark:bg-gray-900" />
          <div className="h-96 rounded-2xl border border-gray-200/80 bg-white lg:col-span-2 dark:border-gray-800 dark:bg-gray-900" />
        </div>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="space-y-6">
        <PageHeader title="Profile" description="Your CloudNest account details." />
        <ErrorState
          title="Couldn't load your profile"
          message="Your profile information couldn't be fetched right now."
          onRetry={() => void refetch()}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Profile" description="Your CloudNest account details." />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* ── Account summary ─────────────────────────────────────────────── */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="space-y-4"
        >
          <Card>
            <CardBody className="flex flex-col items-center text-center">
              <div className="relative">
                <Avatar name={displayName} avatarUrl={profile.avatarUrl} size="xl" />
                <button
                  type="button"
                  onClick={() => avatarInputRef.current?.click()}
                  aria-label="Upload avatar"
                  title="Upload avatar"
                  disabled={updateProfile.isPending}
                  className="absolute right-0 bottom-0 grid h-9 w-9 cursor-pointer place-items-center rounded-full border border-gray-200 bg-white text-gray-600 shadow-md transition-colors hover:bg-gray-50 hover:text-brand-600 disabled:opacity-60 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <Camera className="h-4 w-4" />
                </button>
                <input
                  ref={avatarInputRef}
                  type="file"
                  accept="image/*"
                  className="sr-only"
                  onChange={(event) => {
                    void handleAvatarFile(event.target.files?.[0]);
                    event.target.value = '';
                  }}
                />
              </div>

              <h2 className="mt-4 flex items-center gap-1.5 text-xl font-bold text-gray-900 dark:text-white">
                {displayName}
                {isAdminRole(profile.role) && (
                  <BadgeCheck className="text-brand-500 h-5 w-5" aria-label="Admin" />
                )}
              </h2>
              <p className="mt-0.5 flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
                <AtSign className="h-3.5 w-3.5" />
                {profile.username}
              </p>

              <span className="bg-brand-500/10 text-brand-600 dark:text-brand-300 mt-3 rounded-full px-3 py-1 text-xs font-semibold tracking-wide uppercase">
                {profile.role}
              </span>

              <div className="mt-5 w-full space-y-2.5 border-t border-gray-100 pt-4 text-left text-sm dark:border-gray-800">
                <p className="flex items-center gap-2.5 text-gray-600 dark:text-gray-300">
                  <Mail className="h-4 w-4 shrink-0 text-gray-400" />
                  <span className="truncate">{profile.email}</span>
                </p>
                <p className="flex items-center gap-2.5 text-gray-600 dark:text-gray-300">
                  <CalendarDays className="h-4 w-4 shrink-0 text-gray-400" />
                  Member since {formatFileDate(profile.createdAt)}
                </p>
                <p className="flex items-center gap-2.5 text-gray-600 dark:text-gray-300">
                  <History className="h-4 w-4 shrink-0 text-gray-400" />
                  Last login{' '}
                  {profile.lastLogin ? formatRelativeTime(profile.lastLogin) : 'not recorded yet'}
                </p>
              </div>
            </CardBody>
          </Card>
        </motion.div>

        {/* ── Edit profile form ───────────────────────────────────────────── */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.08 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader
              title="Edit profile"
              description="Update the details shown across CloudNest."
            />
            <CardBody>
              <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                  <Input
                    label="Display name"
                    type="text"
                    placeholder="Jane Doe"
                    autoComplete="name"
                    error={errors.displayName?.message}
                    {...register('displayName', {
                      required: 'Display name is required',
                      minLength: {
                        value: NAME_MIN_LENGTH,
                        message: `Name must be at least ${NAME_MIN_LENGTH} characters`,
                      },
                    })}
                  />
                  <Input
                    label="Email"
                    type="email"
                    placeholder="you@example.com"
                    autoComplete="email"
                    error={errors.email?.message}
                    {...register('email', {
                      required: 'Email is required',
                      pattern: { value: EMAIL_PATTERN, message: 'Enter a valid email address' },
                    })}
                  />
                </div>

                <Input
                  label="Phone"
                  type="tel"
                  placeholder="+1 555 000 1234"
                  autoComplete="tel"
                  error={errors.phone?.message}
                  {...register('phone')}
                />

                <div>
                  <label
                    htmlFor="profile-bio"
                    className="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-200"
                  >
                    Bio
                  </label>
                  <textarea
                    id="profile-bio"
                    rows={4}
                    placeholder="A few words about you…"
                    className={cn(
                      'w-full resize-none rounded-lg border bg-white px-3.5 py-2.5 text-sm text-gray-900 shadow-sm transition-colors placeholder:text-gray-400',
                      'focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25',
                      'border-gray-300 dark:border-gray-700 dark:bg-gray-950 dark:text-white dark:placeholder:text-gray-500',
                    )}
                    {...register('bio', { maxLength: 500 })}
                  />
                  <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400">
                    {errors.bio?.message ?? 'A short bio shown on your profile.'}
                  </p>
                </div>

                <div className="flex justify-end">
                  <Button
                    type="submit"
                    leftIcon={<Save className="h-4 w-4" />}
                    isLoading={updateProfile.isPending}
                    disabled={!isDirty}
                  >
                    Save changes
                  </Button>
                </div>
              </form>
            </CardBody>
          </Card>
        </motion.div>
      </div>

      {/* ── Storage + recent activity ────────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.12 }}
        >
          <Card>
            <CardHeader
              title="Storage"
              description="How your cloud is used."
              action={<HardDrive className="h-5 w-5 text-gray-400" />}
            />
            <CardBody>
              {stats.isLoading ? (
                <div className="space-y-3 animate-pulse">
                  <div className="h-4 w-24 rounded-full bg-gray-100 dark:bg-gray-800" />
                  <div className="h-2.5 w-full rounded-full bg-gray-100 dark:bg-gray-800" />
                </div>
              ) : (
                <>
                  <div className="flex items-baseline justify-between">
                    <p className="text-lg font-bold tracking-tight text-gray-900 dark:text-white">
                      {formatBytes(stats.storageUsed)}
                    </p>
                    <p className="text-xs text-gray-400 dark:text-gray-500">
                      of {formatBytes(stats.storageQuota)} ({storagePct}%)
                    </p>
                  </div>
                  <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
                    <motion.div
                      className="from-brand-500 to-accent-600 h-full rounded-full bg-linear-to-r"
                      initial={{ width: 0 }}
                      animate={{ width: `${storagePct}%` }}
                      transition={{ duration: 0.6, ease: 'easeOut' }}
                    />
                  </div>

                  <ul className="mt-5 space-y-2.5 text-sm">
                    <li className="flex items-center justify-between">
                      <span className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                        <Files className="h-4 w-4 text-gray-400" /> Files
                      </span>
                      <span className="font-medium text-gray-900 dark:text-white">
                        {stats.filesCount.toLocaleString()}
                      </span>
                    </li>
                    <li className="flex items-center justify-between">
                      <span className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                        <FolderOpen className="h-4 w-4 text-gray-400" /> Folders
                      </span>
                      <span className="font-medium text-gray-900 dark:text-white">
                        {stats.foldersCount.toLocaleString()}
                      </span>
                    </li>
                  </ul>

                  <p className="mt-4 rounded-lg bg-gray-50 px-3 py-2 text-xs text-gray-500 dark:bg-gray-800/60 dark:text-gray-400">
                    Your free tier includes {formatBytes(stats.storageQuota)} of storage.
                  </p>
                </>
              )}
            </CardBody>
          </Card>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.16 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader
              title="Recent activity"
              description="Your latest uploads and changes."
              action={<History className="h-5 w-5 text-gray-400" />}
            />
            <CardBody className="p-0">
              {stats.isLoading ? (
                <div className="space-y-3 p-6">
                  {Array.from({ length: 4 }).map((_, index) => (
                    <div
                      key={index}
                      className="flex animate-pulse items-center gap-3.5 rounded-xl bg-gray-50 p-3 dark:bg-gray-800/50"
                    >
                      <div className="h-10 w-10 rounded-xl bg-gray-100 dark:bg-gray-800" />
                      <div className="flex-1 space-y-2">
                        <div className="h-3 w-1/2 rounded-full bg-gray-100 dark:bg-gray-800" />
                        <div className="h-2.5 w-16 rounded-full bg-gray-100 dark:bg-gray-800" />
                      </div>
                    </div>
                  ))}
                </div>
              ) : stats.recentFiles.length === 0 ? (
                <div className="flex flex-col items-center gap-2 px-6 py-10 text-center">
                  <span className="bg-brand-500/10 text-brand-500 grid h-12 w-12 place-items-center rounded-2xl">
                    <Upload className="h-6 w-6" />
                  </span>
                  <p className="text-sm font-medium text-gray-900 dark:text-white">
                    No activity yet
                  </p>
                  <p className="text-sm text-gray-400 dark:text-gray-500">
                    Upload your first file and it will show up here.
                  </p>
                </div>
              ) : (
                <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                  {stats.recentFiles.map((file, index) => (
                    <motion.li
                      key={file.id}
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.18, delay: Math.min(index * 0.04, 0.3) }}
                      className="flex items-center gap-3.5 px-5 py-3"
                    >
                      <FileIcon file={file} size="md" />
                      <div className="min-w-0 flex-1">
                        <p
                          title={file.originalFileName}
                          className="truncate text-sm font-medium text-gray-900 dark:text-white"
                        >
                          {file.originalFileName}
                          {isPdfFile(file) && (
                            <span className="text-rose-500 ml-1.5 text-[10px] font-bold tracking-wide uppercase">
                              PDF
                            </span>
                          )}
                        </p>
                        <p className="text-xs text-gray-400 dark:text-gray-500">
                          {formatBytes(file.fileSize)}
                        </p>
                      </div>
                      <span className="shrink-0 text-xs text-gray-400 dark:text-gray-500">
                        {formatRelativeTime(file.createdAt)}
                      </span>
                    </motion.li>
                  ))}
                </ul>
              )}
            </CardBody>
          </Card>
        </motion.div>
      </div>

      {/* ── Security ──────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.2 }}
      >
        <SecurityCard />
      </motion.div>
    </div>
  );
}

/** Change-password card — verifies the current password via the auth-service. */
function SecurityCard() {
  const changePassword = useChangePasswordMutation();

  const {
    register,
    handleSubmit,
    reset,
    getValues,
    formState: { errors, isDirty },
  } = useForm<ChangePasswordFormValues>({ defaultValues: EMPTY_PASSWORD_FORM });

  const onSubmit = (values: ChangePasswordFormValues) => {
    changePassword.mutate(
      { currentPassword: values.currentPassword, newPassword: values.newPassword },
      { onSuccess: () => reset() },
    );
  };

  return (
    <Card>
      <CardHeader
        title="Security"
        description="Update your password regularly to keep your account safe."
        action={<ShieldCheck className="h-5 w-5 text-gray-400" />}
      />
      <CardBody>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
          <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
            <PasswordInput
              label="Current password"
              placeholder="Your current password"
              autoComplete="current-password"
              leftIcon={<KeyRound className="h-4 w-4" />}
              error={errors.currentPassword?.message}
              {...register('currentPassword', {
                required: 'Current password is required',
              })}
            />
            <PasswordInput
              label="New password"
              placeholder="Create a new password"
              autoComplete="new-password"
              leftIcon={<KeyRound className="h-4 w-4" />}
              hint={PASSWORD_REQUIREMENTS_MESSAGE}
              error={errors.newPassword?.message}
              {...register('newPassword', {
                required: 'New password is required',
                minLength: {
                  value: PASSWORD_MIN_LENGTH,
                  message: `Password must be at least ${PASSWORD_MIN_LENGTH} characters`,
                },
                pattern: { value: PASSWORD_PATTERN, message: PASSWORD_REQUIREMENTS_MESSAGE },
              })}
            />
            <PasswordInput
              label="Confirm new password"
              placeholder="Repeat your new password"
              autoComplete="new-password"
              leftIcon={<KeyRound className="h-4 w-4" />}
              error={errors.confirmPassword?.message}
              {...register('confirmPassword', {
                required: 'Please confirm your new password',
                validate: (value) => value === getValues('newPassword') || 'Passwords do not match',
              })}
            />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="flex items-center gap-1.5 text-xs text-gray-400 dark:text-gray-500">
              <Lock className="h-3.5 w-3.5" />
              You'll use your new password the next time you sign in.
            </p>
            <Button
              type="submit"
              leftIcon={<KeyRound className="h-4 w-4" />}
              isLoading={changePassword.isPending}
              disabled={!isDirty}
            >
              Update password
            </Button>
          </div>
        </form>
      </CardBody>
    </Card>
  );
}
