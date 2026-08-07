import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, AppNotification } from '@/types';

/** Notification service (notification-service). */
export const notificationService = {
  getNotifications: () =>
    apiClient.get<ApiResponse<AppNotification[]>>(API_ENDPOINTS.notifications.list),

  getUnreadCount: () =>
    apiClient.get<ApiResponse<{ count: number }>>(API_ENDPOINTS.notifications.unreadCount),

  markAsRead: (id: number) =>
    apiClient.put<ApiResponse<AppNotification>>(API_ENDPOINTS.notifications.markAsRead(id)),

  markAllAsRead: () =>
    apiClient.put<ApiResponse<null>>(API_ENDPOINTS.notifications.markAllAsRead),

  deleteNotification: (id: number) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.notifications.remove(id)),
};
