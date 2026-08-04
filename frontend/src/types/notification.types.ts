/**
 * Notification types (notification-service).
 *
 * Named `AppNotification` to avoid clashing with the global `Notification` API.
 */

export type NotificationType = 'SHARE' | 'SYSTEM' | 'ALERT' | 'INFO';

export interface AppNotification {
  id: string;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
}
