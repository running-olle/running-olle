export type GeoPoint = {
  latitude: number
  longitude: number
  accuracy?: number
  timestamp?: number
}

export type RunningPhase = 'running' | 'paused'
export type RunningMode = 'COURSE_SELECT' | 'COURSE_CREATE' | 'FREE_RUN'

export type SavedRunningRecord = {
  localId: string
  serverId?: string
  courseId: string | null
  courseName: string | null
  runningMode: RunningMode
  distanceMeters: number
  durationSeconds: number
  averagePace: number | null
  calories: number
  startedAt: string
  endedAt: string
  route: GeoPoint[]
  syncStatus: 'synced' | 'pending'
}
