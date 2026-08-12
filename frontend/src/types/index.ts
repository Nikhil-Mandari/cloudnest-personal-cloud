/**
 * Barrel export for all domain types.
 *
 * NOTE: minimal reconstruction — exports the type modules present on this
 * branch. All recovery domain type modules (api / auth / user / folder /
 * explorer / file / fileAdvanced / share / notification / admin) are now
 * exported — the barrel is complete.
 */
export type { ApiErrorResponse, ApiResponse, PaginatedResponse } from './api.types';
export type {
  AccountStatus,
  AuthResponse,
  DisableTwoFactorRequest,
  EnableTwoFactorRequest,
  EnableTwoFactorResponse,
  ForgotPasswordRequest,
  LoginFormValues,
  LoginHistoryEntry,
  LoginRequest,
  LoginResponse,
  OtpDispatchResponse,
  OtpPurpose,
  PasskeyAuthenticationFinishRequest,
  PasskeyAuthenticationStart,
  PasskeyCredentialInfo,
  PasskeyRegistrationFinishRequest,
  PasskeyRegistrationStart,
  RefreshTokenRequest,
  RegenerateBackupCodesResponse,
  RegisterFormValues,
  RegisterRequest,
  RegisterResponse,
  ResendOtpRequest,
  ResetPasswordRequest,
  ResetTokenResponse,
  SecurityLogEntry,
  SecurityOverview,
  SessionInfo,
  TrustedDeviceInfo,
  TwoFactorLoginRequest,
  TwoFactorSetup,
  TwoFactorStatus,
  User,
  UserRole,
  VerifyOtpPurpose,
  VerifyOtpRequest,
  VerifyOtpState,
} from './auth.types';
export type { ChangePasswordRequest, UpdateProfileRequest, UserProfile } from './user.types';
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
export type {
  CreateShareRequest,
  MySharesSortKey,
  ShareAnalytics,
  SharePermission,
  ShareRecord,
  ShareResourceType,
  ShareSortKey,
  ShareTypeFilter,
  UpdateShareRequest,
  VerifySharePasswordRequest,
} from './share.types';
export type { AppNotification, NotificationType } from './notification.types';
export type {
  CreateOrderRequest,
  OrderStatus,
  PaymentOrder,
  Plan,
  PlanType,
  Quota,
  Subscription,
  SubscriptionStatus,
  VerifyPaymentRequest,
} from './billing.types';
export type {
  AdminAuditLogs,
  AdminPagedUsers,
  AdminSecurityOverview,
  AdminStorageOverview,
  AdminTab,
  AdminUserCredential,
  AdminUserSummary,
  MinioStatus,
  ServiceHealth,
  SystemHealth,
} from './admin.types';
