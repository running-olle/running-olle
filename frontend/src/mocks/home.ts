import type { Weather } from '../features/home/types'

export type Difficulty = 'easy' | 'medium'

export type CourseImageTone = 'beach' | 'oreum' | 'forest'

export type HomeUser = {
  id: string
  nickname: string
  location: string
}

export type RecommendedCourse = {
  id: string
  title: string
  category: string
  distanceKm: number
  estimatedMinutes: number
  rating: number
  ratingCount: number
  difficulty: Difficulty
  location: string
  imageTone: CourseImageTone
}

export const user: HomeUser = {
  id: 'user-1',
  nickname: '러너제주',
  location: '제주 제주시 구좌읍',
}

export const weather: Weather = {
  headline: '오늘 달리기 좋습니다',
  temperatureCelsius: 23,
  condition: '맑음',
  windSpeedMeterPerSecond: 2,
}

export const recommendedCourses: RecommendedCourse[] = [
  {
    id: 'hamdeok-morning',
    title: '함덕 감성 모닝런',
    category: '스팟',
    distanceKm: 3.2,
    estimatedMinutes: 25,
    rating: 4.6,
    ratingCount: 128,
    difficulty: 'easy',
    location: '제주 조천읍 함덕리',
    imageTone: 'beach',
  },
  {
    id: 'seongsan-sunrise',
    title: '성산 일출봉 바다런',
    category: '오름',
    distanceKm: 6.5,
    estimatedMinutes: 42,
    rating: 4.8,
    ratingCount: 96,
    difficulty: 'medium',
    location: '서귀포시 성산읍',
    imageTone: 'oreum',
  },
  {
    id: 'saryeoni-healing',
    title: '사려니숲길 힐링런',
    category: '숲길',
    distanceKm: 8.2,
    estimatedMinutes: 55,
    rating: 4.7,
    ratingCount: 84,
    difficulty: 'medium',
    location: '제주 조천읍 교래리',
    imageTone: 'forest',
  },
]
