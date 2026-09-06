import { axiosInstance } from '../../api/axiosInstance'
import type { Bookmark, Dashboard, NotificationSettings, Profile, RunRecord, ThemeOption, Trip } from './types'
export const myPageService = {
  themes: () => axiosInstance.get<ThemeOption[]>('/themes').then(({ data }) => data),
  dashboard: () => axiosInstance.get<Dashboard>('/mypage').then(({ data }) => data),
  runs: () => axiosInstance.get<RunRecord[]>('/mypage/runs').then(({ data }) => data),
  bookmarks: () => axiosInstance.get<Bookmark[]>('/mypage/bookmarks').then(({ data }) => data),
  removeBookmark: (id: string) => axiosInstance.delete(`/mypage/bookmarks/${id}`),
  trips: () => axiosInstance.get<Trip[]>('/mypage/trips').then(({ data }) => data),
  createTrip: (body: Pick<Trip, 'name' | 'region' | 'startDate' | 'endDate' | 'thumbnailImageUrl'>) => axiosInstance.post<Trip>('/mypage/trips', body).then(({ data }) => data),
  profile: () => axiosInstance.get<Profile>('/users/me/profile').then(({ data }) => data),
  updateProfile: (body: Partial<Profile> & { themeIds?: string[] }) => axiosInstance.put<Profile>('/users/me/profile', body).then(({ data }) => data),
  notifications: () => axiosInstance.get<NotificationSettings>('/users/me/notifications').then(({ data }) => data),
  updateNotifications: (body: NotificationSettings) => axiosInstance.put<NotificationSettings>('/users/me/notifications', body).then(({ data }) => data),
}
