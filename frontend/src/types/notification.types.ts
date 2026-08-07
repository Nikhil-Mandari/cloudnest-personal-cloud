/**
 * Notification types (notification-service).
 *
 * Named `AppNotification` to avoid clashing with the global `Notification` API.
 * The shape mirrors the backend `NotificationResponse` DTO.
 */

export type NotificationType =
  | 'SHARE_RECEIVED'
  | 'SHARE_UPDATED'
  | 'SHARE_REVOKED'
  | 'FILE_SHARED'
  | 'FOLDER_SHARED'
  | 'SYSTEM';

export interface AppNotification {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  message: string;
  relatedResourceId?: string | null;
  relatedResourceType?: string | null;
  isRead: boolean;
  createdAt: string;
}
