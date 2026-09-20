import { axiosInstance } from '../../api/axiosInstance'

export type FeedVisibility = 'PUBLIC' | 'PRIVATE'
export type FeedCourseType = 'RUNNING_COURSE' | 'SPOT_COURSE'

export type FeedSelectionOption = {
  id: string
  label: string
  courseType: FeedCourseType | null
  distanceKm: number | null
  durationSeconds: number | null
}

export type FeedComment = {
  id: string
  userId: string
  nickname: string
  profileImageUrl: string | null
  content: string
  createdAt: string
  mine: boolean
}

export type FeedPost = {
  id: string
  userId: string
  mine: boolean
  nickname: string
  profileImageUrl: string | null
  region: string
  content: string
  visibility: FeedVisibility
  photoTagged: boolean
  likedByMe: boolean
  likeCount: number
  commentCount: number
  createdAt: string
  runningRecord: null | {
    id: string
    distanceKm: number
    durationSeconds: number
  }
  course: null | {
    id: string
    name: string
    courseType: FeedCourseType
  }
  imageUrls: string[]
  comments: FeedComment[]
}

export type FeedPostCreatePayload = {
  runningRecordId: string | null
  courseId: string | null
  content: string
  visibility: FeedVisibility
  region: string
  photoTagged: boolean
  imageUrls: string[]
}

export async function getFeed() {
  const { data } = await axiosInstance.get<FeedPost[]>('/community/feed')
  return data
}

export async function getFeedPost(feedPostId: string) {
  const { data } = await axiosInstance.get<FeedPost>(`/community/feed/${feedPostId}`)
  return data
}

export async function getFeedRunningRecordOptions() {
  const { data } = await axiosInstance.get<FeedSelectionOption[]>('/community/feed/options/running-records')
  return data
}

export async function getFeedCourseOptions() {
  const { data } = await axiosInstance.get<FeedSelectionOption[]>('/community/feed/options/courses')
  return data
}

export async function uploadFeedImages(files: File[]) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))

  const { data } = await axiosInstance.post<{ imageUrls: string[] }>('/community/feed/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return data.imageUrls
}

export async function createFeedPost(payload: FeedPostCreatePayload) {
  const { data } = await axiosInstance.post<FeedPost>('/community/feed', payload)
  return data
}

export async function updateFeedPost(
  feedPostId: string,
  payload: Omit<FeedPostCreatePayload, 'runningRecordId'> & { runningRecordId?: string | null },
) {
  const { data } = await axiosInstance.post<FeedPost>(`/community/feed/${feedPostId}`, payload)
  return data
}

export async function toggleFeedLike(feedPostId: string) {
  const { data } = await axiosInstance.post<{ liked: boolean; likeCount: number }>(
    `/community/feed/${feedPostId}/likes`,
  )
  return data
}

export async function createFeedComment(feedPostId: string, content: string) {
  const { data } = await axiosInstance.post<FeedComment>(`/community/feed/${feedPostId}/comments`, { content })
  return data
}

export async function deleteFeedComment(commentId: string) {
  await axiosInstance.delete(`/community/feed/comments/${commentId}`)
}

export async function deleteFeedPost(feedPostId: string) {
  await axiosInstance.delete(`/community/feed/${feedPostId}`)
}
