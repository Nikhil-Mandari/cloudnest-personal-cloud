import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, AppNotification } from '@/types';

/** Notification service (notification-service). */
export const notificationService = {
  getNotifications: () =>
    apiClient.get<ApiResponse<AppNotification[]>>(API_ENDPOINTS.notifications.list),

  markAsRead: (id: string) =>
    apiClient.put<ApiResponse<AppNotification>>(API_ENDPOINTS.notifications.markAsRead(id)),
};
