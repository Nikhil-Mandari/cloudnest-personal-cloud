import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  AdminAuditLogs,
  AdminPagedUsers,
  AdminSecurityOverview,
  AdminStorageOverview,
  AdminUserCredential,
  AdminUserSummary,
  ApiResponse,
  LoginHistoryEntry,
  MinioStatus,
  PaginatedResponse,
  SecurityLogEntry,
  SystemHealth,
} from '@/types';

/** Admin service — platform-wide views (all endpoints require ROLE_ADMIN). */
export const adminService = {
  // ── Gateway ──────────────────────────────────────────────────────────────
  getSystemHealth: () =>
    apiClient.get<ApiResponse<SystemHealth>>(API_ENDPOINTS.admin.systemHealth),

  // ── User service ─────────────────────────────────────────────────────────
  getUserSummary: () =>
    apiClient.get<ApiResponse<AdminUserSummary>>(API_ENDPOINTS.admin.userSummary),

  getUsers: (params: { page?: number; size?: number; query?: string }) =>
    apiClient.get<ApiResponse<AdminPagedUsers>>(API_ENDPOINTS.admin.users, { params }),

  // ── File service ─────────────────────────────────────────────────────────
  getStorageOverview: () =>
    apiClient.get<ApiResponse<AdminStorageOverview>>(API_ENDPOINTS.admin.storageOverview),

  getAuditLogs: (params: { page?: number; size?: number; action?: string; userId?: number }) =>
    apiClient.get<ApiResponse<AdminAuditLogs>>(API_ENDPOINTS.admin.auditLogs, { params }),

  getMinioStatus: () =>
    apiClient.get<ApiResponse<MinioStatus>>(API_ENDPOINTS.admin.minioStatus),

  // ── Auth service ─────────────────────────────────────────────────────────
  getSecurityOverview: () =>
    apiClient.get<ApiResponse<AdminSecurityOverview>>(API_ENDPOINTS.admin.securityOverview),

  getLoginHistory: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PaginatedResponse<LoginHistoryEntry>>>(
      API_ENDPOINTS.admin.loginHistory,
      { params: { page, size } },
    ),

  getSecurityLogs: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PaginatedResponse<SecurityLogEntry>>>(
      API_ENDPOINTS.admin.securityLogs,
      { params: { page, size } },
    ),

  setUserEnabled: (id: number, enabled: boolean) =>
    apiClient.patch<ApiResponse<AdminUserCredential>>(
      API_ENDPOINTS.admin.setUserEnabled(id),
      undefined,
      { params: { enabled } },
    ),

  setUserRole: (id: number, role: 'ROLE_ADMIN' | 'ROLE_USER') =>
    apiClient.patch<ApiResponse<AdminUserCredential>>(
      API_ENDPOINTS.admin.setUserRole(id),
      undefined,
      { params: { role } },
    ),
};
