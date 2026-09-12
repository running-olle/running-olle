import { axiosInstance } from '../../api/axiosInstance'

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
}

type RecommendedCoursesResponse = {
  recommendations: HomeRecommendedCourse[]
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
}
