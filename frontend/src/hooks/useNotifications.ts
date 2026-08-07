import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { notificationService } from '@/services/notification.service';
import { getErrorMessage } from '@/utils/error';

export const NOTIFICATIONS_QUERY_KEY = ['notifications'] as const;
export const NOTIFICATIONS_UNREAD_COUNT_QUERY_KEY = ['notifications', 'unread-count'] as const;

/** How often the notifications list + unread badge poll the API. */
export const NOTIFICATION_POLL_INTERVAL_MS = 30_000;

/** Lists the authenticated user's notifications (newest first). */
export function useNotificationsQuery() {
  return useQuery({
    queryKey: NOTIFICATIONS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await notificationService.getNotifications();
      return data.data;
    },
    refetchInterval: NOTIFICATION_POLL_INTERVAL_MS,
  });
}

/** Live unread count, polled for the navbar badge. */
export function useUnreadCountQuery() {
  return useQuery({
    queryKey: NOTIFICATIONS_UNREAD_COUNT_QUERY_KEY,
    queryFn: async () => {
      const { data } = await notificationService.getUnreadCount();
      return data.data.count;
    },
    refetchInterval: NOTIFICATION_POLL_INTERVAL_MS,
  });
}

/**
 * Notification mutations (mark read, mark all read, delete). Each success
 * refreshes both the list and the unread badge.
 */
export function useNotificationMutations() {
  const queryClient = useQueryClient();

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_QUERY_KEY });
    void queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_UNREAD_COUNT_QUERY_KEY });
  };

  const markAsRead = useMutation({
    mutationFn: (id: number) => notificationService.markAsRead(id),
    onSuccess: () => {
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to mark as read.')),
  });

  const markAllAsRead = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      toast.success('All notifications marked as read');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to mark notifications as read.')),
  });

  const deleteNotification = useMutation({
    mutationFn: (id: number) => notificationService.deleteNotification(id),
    onSuccess: () => {
      toast.success('Notification deleted');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to delete the notification.')),
  });

  const clearRead = useMutation({
    mutationFn: () => notificationService.clearRead(),
    onSuccess: () => {
      toast.success('Read notifications cleared');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to clear notifications.')),
  });

  return { markAsRead, markAllAsRead, deleteNotification, clearRead };
}
