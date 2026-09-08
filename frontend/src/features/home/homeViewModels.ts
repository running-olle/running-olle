import type { CourseImageTone } from '../../mocks/home'
import type { HomeRecommendedCourse, HomeRecommendedCourseDifficulty } from './homeService'

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

const themeLabelMap: Record<string, string> = {
  COAST: '해안',
  FOREST: '숲길',
  OREUM: '오름',
  FOOD: '미식',
  PHOTO: '포토',
  TRADITION: '전통',
  URBAN: '도심',
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
  const matchedTheme = themes.find((theme) => themeLabelMap[theme])
  return matchedTheme ? themeLabelMap[matchedTheme] : '추천'
}

function imageTone(themes: string[]): CourseImageTone {
  if (themes.includes('COAST') || themes.includes('PHOTO')) return 'beach'
  if (themes.includes('OREUM')) return 'oreum'
  return 'forest'
}
