/**
 * Barrel export for all domain types.
 *
 * NOTE: minimal reconstruction — exports only the type modules present on
 * this branch (api / auth / folder / explorer / file / fileAdvanced). The
 * remaining domain type modules (share, notification, admin, user) are added
 * incrementally as their slices land.
 */
export type { ApiErrorResponse, ApiResponse, PaginatedResponse } from './api.types';
export type {
  AuthResponse,
  LoginFormValues,
  LoginRequest,
  RegisterFormValues,
  RegisterRequest,
  User,
  UserRole,
} from './auth.types';
export type { CreateFolderRequest, Folder, FolderSortKey } from './folder.types';
export type {
  FileTypeCategory,
  FileTypeFilter,
  FileViewMode,
  SortDirection,
  SortKey,
  SortState,
} from './explorer.types';
export type {
  DuplicateAction,
  DuplicateFileInfo,
  FileDetail,
  FileItem,
  FileStatus,
  ScanStatus,
  UploadResult,
} from './file.types';
export type {
  AuditLogEntry,
  DownloadZipRequest,
  FileTypeStat,
  FileVersion,
  LargestFileInfo,
  PagedAuditLogs,
  ScanStatusInfo,
  StorageOverview,
  UsagePoint,
} from './fileAdvanced.types';
