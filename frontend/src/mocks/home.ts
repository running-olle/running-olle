export type Difficulty = 'easy' | 'medium'

export type CourseImageTone = 'beach' | 'oreum' | 'forest'

export type HomeUser = {
  id: string
  nickname: string
  location: string
}

export type Weather = {
  headline: string
  temperatureCelsius: number
  condition: string
  windSpeedMeterPerSecond: number
  runningNowCount: number
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

export type PopularCourse = {
  id: string
  rank: number
  title: string
  distanceKm: number
  difficulty: Difficulty
  participantCount: number
  imageTone: CourseImageTone
}

export type RunningEvent = {
  id: string
  month: string
  day: string
  title: string
}

export const user: HomeUser = {
  id: 'user-1',
  nickname: '러너제주',
  location: '제주시 구좌읍',
}

export const weather: Weather = {
  headline: '오늘 달리기 좋아요 ✨',
  temperatureCelsius: 23,
  condition: '맑음',
  windSpeedMeterPerSecond: 2,
  runningNowCount: 12,
}

export const recommendedCourses: RecommendedCourse[] = [
  {
    id: 'hamdeok-morning',
    title: '함덕 감성 모닝런',
    category: '학팟',
    distanceKm: 3.2,
    estimatedMinutes: 25,
    rating: 4.6,
    ratingCount: 128,
    difficulty: 'easy',
    location: '제주시 조천읍 함덕리',
    imageTone: 'beach',
  },
  {
    id: 'seongsan-sunrise',
    title: '성산 일출봉 바닷길',
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
    location: '제주시 조천읍 교래리',
    imageTone: 'forest',
  },
]

export const popularCourses: PopularCourse[] = [
  {
    id: 'seopjikoji-coast',
    rank: 1,
    title: '섭지코지 해안런',
    distanceKm: 5.6,
    difficulty: 'medium',
    participantCount: 24,
    imageTone: 'beach',
  },
  {
    id: 'saryeoni-healing-rank',
    rank: 2,
    title: '사려니숲길 힐링런',
    distanceKm: 8.2,
    difficulty: 'easy',
    participantCount: 18,
    imageTone: 'forest',
  },
  {
    id: 'hamdeok-morning-rank',
    rank: 3,
    title: '함덕 감성 모닝런',
    distanceKm: 3.2,
    difficulty: 'easy',
    participantCount: 12,
    imageTone: 'oreum',
  },
]

export const events: RunningEvent[] = [
  {
    id: 'jeju-trail-jun',
    month: 'JUN',
    day: '22',
    title: '제주 국제 트레일러닝',
  },
  {
    id: 'jeju-citrus-jul',
    month: 'JUL',
    day: '05',
    title: '제주 감귤 축제 달리기',
  },
]
