import { axiosInstance } from './axiosInstance'
import type {
  CourseReviewCreateRequest,
  CourseReviewListResponse,
  TestTokenRequest,
  TestTokenResponse,
  CourseReviewUpdateRequest,
} from '../types/courseReview'

export async function getCourseReviews(courseId: string) {
  const response = await axiosInstance.get<CourseReviewListResponse>(
    `/courses/${courseId}/reviews`,
  )
  return response.data
}

export async function createCourseReview(
  courseId: string,
  payload: CourseReviewCreateRequest,
) {
  const response = await axiosInstance.post(
    `/courses/${courseId}/reviews`,
    payload,
  )
  return response.data
}

export async function updateCourseReview(
  courseId: string,
  reviewId: string,
  payload: CourseReviewUpdateRequest,
) {
  const response = await axiosInstance.patch(
    `/courses/${courseId}/reviews/${reviewId}`,
    payload,
  )
  return response.data
}

export async function deleteCourseReview(courseId: string, reviewId: string) {
  await axiosInstance.delete(`/courses/${courseId}/reviews/${reviewId}`)
}

export async function issueTestToken(payload: TestTokenRequest) {
  const response = await axiosInstance.post<TestTokenResponse>(
    '/auth/test-token',
    payload,
  )
  return response.data
}
