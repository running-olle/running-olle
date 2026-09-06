import { axiosInstance } from '../../api/axiosInstance'

export type InAppNotification = {
  id: string
  type: 'FEED_LIKE' | 'FEED_COMMENT' | 'MEETUP_JOIN_REQUEST' | 'MEETUP_JOINED' | 'MEETUP_JOIN_ACCEPTED' | 'MEETUP_JOIN_REJECTED' | 'MEETUP_UPDATED' | 'MEETUP_CANCELLED'
  title: string
  message: string
  actionUrl: string | null
  read: boolean
  createdAt: string
}

export type NotificationList = { notifications: InAppNotification[]; unreadCount: number }

export const notificationApi = {
  list: () => axiosInstance.get<NotificationList>('/notifications').then(({ data }) => data),
  markRead: (id: string) => axiosInstance.patch(`/notifications/${id}/read`),
  markAllRead: () => axiosInstance.patch('/notifications/read-all'),
  clear: () => axiosInstance.delete('/notifications'),
}
