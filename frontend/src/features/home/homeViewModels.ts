import type { CourseImageTone } from '../../mocks/home'
import type { HomeRecommendedCourse, HomeRecommendedCourseDifficulty } from './homeService'
import { isThemeCode, normalizeThemeCode, themeLabels } from '../themes/themeCatalog'

export type RecommendedCourseCardViewModel = {
  id: string
  title: string
  category: string
  distanceKm: number
  rating: number | null
  difficulty: HomeRecommendedCourseDifficulty
  imageTone: CourseImageTone
  distanceFromUserKm: number | null
  recommendationReason: string
}

export function toRecommendedCourseCardViewModel(course: HomeRecommendedCourse): RecommendedCourseCardViewModel {
  return {
    id: course.courseId,
    title: course.courseName,
    category: categoryLabel(course.themes),
    distanceKm: course.distanceKm,
    rating: course.averageRating,
    difficulty: course.difficulty,
    imageTone: imageTone(course.themes),
    distanceFromUserKm: course.distanceFromUserKm,
    recommendationReason: course.recommendationReason,
  }
}

function categoryLabel(themes: string[]) {
  const matchedTheme = themes.find(isThemeCode)
  return matchedTheme ? themeLabels[normalizeThemeCode(matchedTheme)] : '추천'
}

function imageTone(themes: string[]): CourseImageTone {
  if (themes.includes('COAST') || themes.includes('PHOTO')) return 'beach'
  if (themes.includes('OREUM')) return 'oreum'
  return 'forest'
}
