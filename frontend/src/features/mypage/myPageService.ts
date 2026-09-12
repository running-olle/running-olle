import { axiosInstance } from '../../api/axiosInstance'
import type { Bookmark, Dashboard, NotificationSettings, Profile, ProfileUpdate, RunRecord, RunRecordDetail, RunTripOverallStatistics, RunTripReportDetail, RunTripReportStatistics, RunTripReportSummary, SaveRunTripReport, ThemeOption, Visit } from './types'
export const myPageService = {
  themes: () => axiosInstance.get<ThemeOption[]>('/themes').then(({ data }) => data),
  dashboard: () => axiosInstance.get<Dashboard>('/mypage').then(({ data }) => data),
  runs: () => axiosInstance.get<RunRecord[]>('/mypage/runs').then(({ data }) => data),
  run: (id: string) => axiosInstance.get<RunRecordDetail>(`/mypage/runs/${id}`).then(({ data }) => data),
  visits: () => axiosInstance.get<Visit[]>('/mypage/visits').then(({ data }) => data),
  bookmarks: () => axiosInstance.get<Bookmark[]>('/mypage/bookmarks').then(({ data }) => data),
  removeBookmark: (id: string) => axiosInstance.delete(`/mypage/bookmarks/${id}`),
  reports: () => axiosInstance.get<RunTripReportSummary[]>('/mypage/reports').then(({ data }) => data),
  report: (id: string) => axiosInstance.get<RunTripReportDetail>(`/mypage/reports/${id}`).then(({ data }) => data),
  reportStatistics: () => axiosInstance.get<RunTripOverallStatistics>('/mypage/reports/statistics').then(({ data }) => data),
  reportPreview: (startDate: string, endDate: string) => axiosInstance.get<RunTripReportStatistics>('/mypage/reports/preview', { params: { startDate, endDate } }).then(({ data }) => data),
  createReport: (body: SaveRunTripReport) => axiosInstance.post<RunTripReportDetail>('/mypage/reports', body).then(({ data }) => data),
  updateReport: (id: string, body: SaveRunTripReport) => axiosInstance.put<RunTripReportDetail>(`/mypage/reports/${id}`, body).then(({ data }) => data),
  deleteReport: (id: string) => axiosInstance.delete(`/mypage/reports/${id}`),
  uploadReportImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosInstance.post<{ imageUrls: string[] }>('/mypage/reports/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(({ data }) => data.imageUrls[0])
  },
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
