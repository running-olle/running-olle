import axios from 'axios'
import { axiosInstance } from '../../api/axiosInstance'
import type { CourseRouteCoordinate } from '../course/types'

export type HomeRecommendedCourseDifficulty = 'LOW' | 'MID' | 'HIGH'

export type HomeRecommendedCourse = {
  courseId: string
  courseName: string
  distanceKm: number
  difficulty: HomeRecommendedCourseDifficulty
  themes: string[]
  averageRating: number | null
  distanceFromUserKm: number | null
  baseScore: number
  ragScore: number | null
  finalScore: number
  recommendationReason: string
  previewRouteCoordinates: CourseRouteCoordinate[]
}

export type HomePopularCourse = {
  courseId: string
  rank: number
  courseName: string
  distanceKm: number
  difficulty: HomeRecommendedCourseDifficulty
  participantCount: number
  thumbnailImageUrl: string | null
  previewRouteCoordinates: CourseRouteCoordinate[]
}

type RecommendedCoursesResponse = {
  recommendations: HomeRecommendedCourse[]
}

type PopularCoursesResponse = {
  courses: HomePopularCourse[]
}

type RecommendedCoursesParams = {
  latitude?: number
  longitude?: number
}

type MonthlyRunCountResponse = {
  runCount: number
}

export const homeService = {
  getMonthlyRunCount() {
    return axiosInstance
      .get<MonthlyRunCountResponse>('/running-records/monthly-count')
      .then(({ data }) => data.runCount)
  },
  getRecommendedCourses(params: RecommendedCoursesParams = {}) {
    return axiosInstance
      .get<RecommendedCoursesResponse>('/home/recommended-courses', {
        params,
      })
      .then(({ data }) => data.recommendations)
  },
  async getPopularCourses() {
    try {
      const { data } = await axiosInstance.get<PopularCoursesResponse>('/home/popular-courses')
      return data.courses
    } catch (error) {
      // Keep the home screen compatible while the endpoint is absent during a staggered deployment.
      // The current backend contract returns 200 with an empty collection when no ranking exists.
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return []
      }
      throw error
    }
  },
}
