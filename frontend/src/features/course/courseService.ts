import { axiosInstance } from '../../api/axiosInstance'
import type { CourseBookmarkResponse, CourseDetail, CourseListFilter, CourseListItem, CourseListScope, CourseReview, CourseReviewInput } from './types'

type GetCoursesParams = {
  filter: CourseListFilter
  scope: CourseListScope
  keyword?: string
}

export const courseService = {
  getCourses({ filter, scope, keyword }: GetCoursesParams) {
    return axiosInstance.get<CourseListItem[]>('/courses', {
      params: { filter, scope, keyword: keyword || undefined },
    }).then(({ data }) => data)
  },

  getCourse(courseId: string) {
    return axiosInstance.get<CourseDetail>(`/courses/${courseId}`).then(({ data }) => data)
  },

  bookmarkCourse(courseId: string) {
    return axiosInstance.post<CourseBookmarkResponse>(`/courses/${courseId}/bookmark`).then(({ data }) => data)
  },

  unbookmarkCourse(courseId: string) {
    return axiosInstance.delete(`/courses/${courseId}/bookmark`)
  },

  deleteCourse(courseId: string) {
    return axiosInstance.delete(`/courses/${courseId}`)
  },

  getReviews(courseId: string) {
    return axiosInstance.get<CourseReview[]>(`/courses/${courseId}/reviews`).then(({ data }) => data)
  },

  createReview(courseId: string, runningRecordId: string, input: CourseReviewInput) {
    return axiosInstance.post<CourseReview>(`/courses/${courseId}/reviews`, {
      runningRecordId,
      ...input,
    }).then(({ data }) => data)
  },

  updateReview(courseId: string, reviewId: string, input: CourseReviewInput) {
    return axiosInstance.patch<CourseReview>(`/courses/${courseId}/reviews/${reviewId}`, input)
      .then(({ data }) => data)
  },

  deleteReview(courseId: string, reviewId: string) {
    return axiosInstance.delete(`/courses/${courseId}/reviews/${reviewId}`)
  },
}
