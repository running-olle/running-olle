import type { Course } from '../models/running'

export const DEUNGCHON_COURSE: Course = {
  id: 'deungchon-station-demo-loop',
  name: '등촌역 블록 러닝',
  description: '등촌역 인근의 짧은 GPS 테스트용 순환 코스',
  distanceLabel: '약 1.1 km',
  estimatedTime: '15분',
  difficulty: '쉬움',
  path: [
    { latitude: 37.55043, longitude: 126.86558 },
    { latitude: 37.55118, longitude: 126.86549 },
    { latitude: 37.55211, longitude: 126.86539 },
    { latitude: 37.55219, longitude: 126.86708 },
    { latitude: 37.55228, longitude: 126.86864 },
    { latitude: 37.55128, longitude: 126.86872 },
    { latitude: 37.55025, longitude: 126.86878 },
    { latitude: 37.55018, longitude: 126.86720 },
    { latitude: 37.55043, longitude: 126.86558 },
  ],
}

export const GANGNAM_HOLLYS_COURSE: Course = {
  id: 'gangnam-hollys-demo-loop',
  name: '할리스 강남역점 미니 러닝',
  description: '할리스 강남역점에서 시작하는 짧은 GPS 테스트용 코스',
  distanceLabel: '약 0.4 km',
  estimatedTime: '5~7분',
  difficulty: '쉬움',
  path: [
    { latitude: 37.49862, longitude: 127.02763 },
    { latitude: 37.49925, longitude: 127.02765 },
    { latitude: 37.49925, longitude: 127.02875 },
    { latitude: 37.49835, longitude: 127.02872 },
    { latitude: 37.49835, longitude: 127.02765 },
    { latitude: 37.49862, longitude: 127.02763 },
  ],
}

export const DEMO_COURSES = [GANGNAM_HOLLYS_COURSE, DEUNGCHON_COURSE]
