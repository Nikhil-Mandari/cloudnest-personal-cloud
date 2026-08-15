import { STORAGE_KEYS } from '@/constants/storage';

/**
 * Device fingerprinting helpers for the enterprise auth flows.
 *
 * The device id is a UUID generated once and persisted — it is what the
 * backend uses to recognise a trusted device across sessions, so it must
 * stay stable for the browser profile.
 */

/**
 * Returns (and lazily creates) the stable device id for this browser.
 */
export function getDeviceId(): string {
  try {
    let id = window.localStorage.getItem(STORAGE_KEYS.deviceId);
    if (!id) {
      id = generateUuid();
      window.localStorage.setItem(STORAGE_KEYS.deviceId, id);
    }
    return id;
  } catch {
    // Private mode / quota — fall back to an in-memory id for this tab.
    return generateUuid();
  }
}

/** Browser family from the User-Agent (mirrors the server-side parser). */
export function detectBrowser(ua = navigator.userAgent): string {
  if (/Edg\//.test(ua)) return 'Edge';
  if (/OPR\/|Opera/.test(ua)) return 'Opera';
  if (/Chrome\//.test(ua)) return 'Chrome';
  if (/Firefox\//.test(ua)) return 'Firefox';
  if (/Safari\//.test(ua)) return 'Safari';
  return 'Unknown';
}

/** Operating system from the User-Agent. */
export function detectOs(ua = navigator.userAgent): string {
  if (/Windows/.test(ua)) return 'Windows';
  if (/Android/.test(ua)) return 'Android';
  if (/iPhone|iPad|iOS/.test(ua)) return 'iOS';
  if (/Mac OS X|Macintosh/.test(ua)) return 'macOS';
  if (/Linux/.test(ua)) return 'Linux';
  if (/CrOS/.test(ua)) return 'ChromeOS';
  return 'Unknown';
}

/** Human-readable device name, e.g. "Chrome on Windows". */
export function getDeviceName(): string {
  const browser = detectBrowser();
  const os = detectOs();
  if (browser === 'Unknown' && os === 'Unknown') return 'This device';
  return os === 'Unknown' ? browser : `${browser} on ${os}`;
}

function generateUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  // Fallback for older browsers.
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0;
    const value = char === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}
