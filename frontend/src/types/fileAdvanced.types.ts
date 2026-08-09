/**
 * Advanced file-management types (file-service): version history, storage
 * analytics, audit trail and bulk ZIP download.
 */

import type { ScanStatus } from './file.types';

/** An archived content snapshot of a file. */
export interface FileVersion {
  /** Version record primary key. */
  id: number;
  /** Sequential version number. */
  versionNumber: number;
  /** Snapshot size in bytes. */
  fileSize: number;
  /** MIME content type. */
  contentType?: string;
  /** SHA-256 checksum of the snapshot content. */
  checksum?: string;
  /** ID of the user who created the version. */
  uploadedBy?: number;
  /** Optional user note. */
  note?: string;
  /** Timestamp when the version was archived. */
  createdAt: string;
}

/** Bytes-uploaded point on a usage timeline. */
export interface UsagePoint {
  label: string;
  bytes: number;
}

/** Storage grouped by file-type category. */
export interface FileTypeStat {
  category: string;
  bytes: number;
  count: number;
}

/** Top file by size. */
export interface LargestFileInfo {
  id: number;
  originalFileName: string;
  fileSize: number;
  fileType?: string;
  folderId?: string | null;
  uploadedAt?: string;
}

/** Full storage analytics overview. */
export interface StorageOverview {
  storageUsed: number;
  fileCount: number;
  folderCount: number;
  trashFileCount: number;
  trashSize: number;
  largestFiles: LargestFileInfo[];
  fileTypeStats: FileTypeStat[];
  weeklyUsage: UsagePoint[];
  monthlyUsage: UsagePoint[];
}

/** A single audit-trail entry. */
export interface AuditLogEntry {
  id: number;
  /** Owner whose action produced the entry (present on admin views). */
  ownerId?: number;
  action: string;
  resourceType?: string;
  resourceId?: string;
  resourceName?: string;
  details?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

/** Paged view of the audit trail. */
export interface PagedAuditLogs {
  content: AuditLogEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Virus-scan status of a single file. */
export interface ScanStatusInfo {
  fileId: string;
  scanStatus: ScanStatus;
}

/** Bulk ZIP download payload (at least one of fileIds / folderIds). */
export interface DownloadZipRequest {
  fileIds?: number[];
  folderIds?: string[];
}
