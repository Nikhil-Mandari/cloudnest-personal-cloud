/**
 * Admin dashboard types (platform-wide views across all microservices).
 */

import type { FileTypeStat, LargestFileInfo, PagedAuditLogs, UsagePoint } from './fileAdvanced.types';
import type { UserProfile } from './user.types';

/** Platform-wide user aggregates (user-service). */
export interface AdminUserSummary {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  adminUsers: number;
  newLast7Days: number;
}

/** Paged user listing (user-service admin view). */
export interface AdminPagedUsers {
  content: UserProfile[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Platform-wide security posture (auth-service). */
export interface AdminSecurityOverview {
  totalAccounts: number;
  activeAccounts: number;
  lockedAccounts: number;
  pendingVerification: number;
  disabledAccounts: number;
  adminCount: number;
  totalLogins: number;
  failedLoginsLast7Days: number;
  activeSessions: number;
  trustedDeviceCount: number;
}

/** Credential snapshot after an admin enable/disable or role change. */
export interface AdminUserCredential {
  id: number;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  status: string;
  lastLoginAt: string | null;
}

/** Platform-wide storage aggregates (file-service). */
export interface AdminStorageOverview {
  totalUsers: number;
  totalFiles: number;
  totalBytes: number;
  trashFileCount: number;
  trashSize: number;
  largestFiles: LargestFileInfo[];
  fileTypeStats: FileTypeStat[];
  weeklyUsage: UsagePoint[];
  monthlyUsage: UsagePoint[];
}

/** Paged platform audit trail (file-service admin view). */
export type AdminAuditLogs = PagedAuditLogs;

/** MinIO object-store status (file-service). */
export interface MinioStatus {
  endpoint: string;
  bucket: string;
  reachable: boolean;
  bucketExists: boolean;
  status: string;
}

/** Health snapshot of a single discovered microservice (gateway). */
export interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'UNKNOWN' | string;
  instanceCount: number;
  instances: string[];
  endpoint: string;
}

/** Aggregated platform health (gateway). */
export interface SystemHealth {
  services: ServiceHealth[];
  healthyCount: number;
  totalCount: number;
  generatedAt: string;
}

/** Admin dashboard tab keys. */
export type AdminTab = 'overview' | 'users' | 'storage' | 'audit' | 'security' | 'system';
