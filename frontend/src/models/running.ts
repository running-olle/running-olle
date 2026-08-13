export type GeoPoint = {
  latitude: number
  longitude: number
  accuracy?: number
  timestamp?: number
}

export type Course = {
  id: string
  name: string
  description: string
  distanceLabel: string
  estimatedTime: string
  difficulty: string
  path: GeoPoint[]
}

export type RouteProgress = {
  percent: number
  distanceFromRouteMeters: number
  completedPath: GeoPoint[]
}

export type RunPhase = 'ready' | 'running' | 'paused' | 'summary'
