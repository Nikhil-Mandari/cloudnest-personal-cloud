/**
 * Formatting helpers shared across the app.
 */

import { formatFileDate } from '@/utils/file';

/**
 * Serializes a Date for backend {@code LocalDateTime} fields.
 *
 * Jackson's JavaTimeModule parses {@code LocalDateTime} with the ISO local
 * formatter, which rejects the trailing "Z" that {@code Date.toISOString()}
 * produces — so the zone suffix is stripped here.
 */
export function toLocalDateTimeIso(date: Date): string {
  return date.toISOString().replace(/\.\d{3}Z$/, '');
}

export function formatBytes(bytes: number, decimals = 1): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return '0 B';
  }
  if (bytes === 0) {
    return '0 B';
  }
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'] as const;
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1);
  return `${parseFloat((bytes / k ** index).toFixed(decimals))} ${sizes[index]}`;
}

/** "Nikhil Mandari" -> "NM" */
export function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const initials = parts
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
  return initials || 'U';
}

/**
 * Formats a byte rate as a human speed, e.g. 1_250_000 -> "1.2 MB/s".
 */
export function formatUploadSpeed(bytesPerSecond: number): string {
  if (!Number.isFinite(bytesPerSecond) || bytesPerSecond <= 0) {
    return '—';
  }
  return `${formatBytes(bytesPerSecond)}/s`;
}

/**
 * Formats a remaining-duration in seconds as "12s" / "1m 05s" / "2m" /
 * "1h 05m" for upload ETAs.
 */
export function formatEta(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '—';
  }
  const total = Math.ceil(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  if (hours > 0) {
    return `${hours}h ${String(minutes).padStart(2, '0')}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${String(secs).padStart(2, '0')}s`;
  }
  return `${secs}s`;
}

/**
 * Human-relative timestamp for feeds ("just now", "5m ago", "2h ago",
 * "yesterday", "3d ago"), falling back to a short date for older entries.
 */
export function formatRelativeTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }

  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return 'just now';

  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;

  const days = Math.floor(hours / 24);
  if (days === 1) return 'yesterday';
  if (days < 7) return `${days}d ago`;

  return formatFileDate(iso);
}
