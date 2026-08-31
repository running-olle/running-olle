import { axiosInstance } from '../../api/axiosInstance'
import type { Bookmark, Dashboard, NotificationSettings, Profile, ProfileUpdate, RunRecord, RunRecordDetail, Trip, Visit } from './types'
export const myPageService = {
  dashboard: () => axiosInstance.get<Dashboard>('/mypage').then(({ data }) => data),
  runs: () => axiosInstance.get<RunRecord[]>('/mypage/runs').then(({ data }) => data),
  run: (id: string) => axiosInstance.get<RunRecordDetail>(`/mypage/runs/${id}`).then(({ data }) => data),
  visits: () => axiosInstance.get<Visit[]>('/mypage/visits').then(({ data }) => data),
  bookmarks: () => axiosInstance.get<Bookmark[]>('/mypage/bookmarks').then(({ data }) => data),
  removeBookmark: (id: string) => axiosInstance.delete(`/mypage/bookmarks/${id}`),
  trips: () => axiosInstance.get<Trip[]>('/mypage/trips').then(({ data }) => data),
  createTrip: (body: Pick<Trip, 'name' | 'region' | 'startDate' | 'endDate' | 'thumbnailImageUrl'>) => axiosInstance.post<Trip>('/mypage/trips', body).then(({ data }) => data),
  profile: () => axiosInstance.get<Profile>('/users/me/profile').then(({ data }) => data),
  updateProfile: (body: ProfileUpdate) => axiosInstance.put<Profile>('/users/me/profile', body).then(({ data }) => data),
  uploadProfileImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosInstance.post<{ imageUrls: string[] }>('/users/me/profile/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(({ data }) => data.imageUrls[0])
  },
  notifications: () => axiosInstance.get<NotificationSettings>('/users/me/notifications').then(({ data }) => data),
  updateNotifications: (body: NotificationSettings) => axiosInstance.put<NotificationSettings>('/users/me/notifications', body).then(({ data }) => data),
}
