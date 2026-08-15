import { useEffect, useState } from 'react';

import { fileService } from '@/services/file.service';
import { isAvatarFileId } from '@/utils/file';

/**
 * Module-level avatar caches shared by every avatar renderer (profile, navbar,
 * user menu). Blob URLs are created once per avatar reference and reused for
 * the whole session; in-flight fetches are deduplicated so two components
 * showing the same avatar only trigger one request.
 */
const cache = new Map<string, string>();
const pending = new Map<string, Promise<string | null>>();

/**
 * Resolves an avatar reference to a renderable URL (object URL for uploaded
 * avatars stored as a file id, or the reference itself for plain URLs).
 * Returns `null` when the image cannot be fetched.
 */
function resolveAvatar(avatarUrl: string): Promise<string | null> {
  if (!isAvatarFileId(avatarUrl)) {
    return Promise.resolve(avatarUrl);
  }
  const cached = cache.get(avatarUrl);
  if (cached) {
    return Promise.resolve(cached);
  }
  const inFlight = pending.get(avatarUrl);
  if (inFlight) {
    return inFlight;
  }
  const request = fileService
    .downloadFile(Number(avatarUrl))
    .then(({ data }) => {
      const objectUrl = URL.createObjectURL(data);
      cache.set(avatarUrl, objectUrl);
      return objectUrl;
    })
    .catch(() => null)
    .finally(() => pending.delete(avatarUrl));
  pending.set(avatarUrl, request);
  return request;
}

/** Reads the resolved URL for a reference without triggering a fetch. */
function peekAvatar(avatarUrl: string | null | undefined): string | null {
  if (!avatarUrl) {
    return null;
  }
  return cache.get(avatarUrl) ?? (isAvatarFileId(avatarUrl) ? null : avatarUrl);
}

/**
 * Resolves a `User.avatarUrl` into something an `<img>` can render:
 *
 * - a numeric file id (avatar uploaded through the file-service) → fetches
 *   the image blob via the authenticated file-service API and caches its
 *   object URL;
 * - a plain http(s) URL → returned as-is;
 * - anything else / fetch failure → `null` (callers fall back to initials).
 */
export function useAvatar(avatarUrl: string | null | undefined): string | null {
  const [url, setUrl] = useState<string | null>(() => peekAvatar(avatarUrl));
  // Tracks the previously-seen reference so a change (e.g. a different user's
  // avatar, or an avatar re-upload) never flashes the old image. Implemented
  // with the documented "adjust state during render" pattern.
  const [previousAvatar, setPreviousAvatar] = useState<string | null | undefined>(avatarUrl);

  if (avatarUrl !== previousAvatar) {
    setPreviousAvatar(avatarUrl);
    setUrl(peekAvatar(avatarUrl));
  }

  useEffect(() => {
    if (!avatarUrl) {
      return;
    }
    let cancelled = false;
    void resolveAvatar(avatarUrl).then((resolved) => {
      if (!cancelled) {
        setUrl(resolved);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [avatarUrl]);

  return url;
}
