export type CourseType = 'RUNNING_COURSE' | 'SPOT_COURSE'
export type CourseDifficulty = 'LOW' | 'MID' | 'HIGH'
export type CourseListFilter = 'ALL' | CourseType | 'CREATED'
export type CourseListScope = 'AVAILABLE' | 'LIBRARY'

export type CourseRouteCoordinate = {
  lat: number
  lng: number
}

export type CourseWaypoint = {
  id: string
  name: string
  kakaoPlaceId: string | null
  lat: number
  lng: number
  orderIndex: number
  distanceFromStartKm: number | null
  description: string | null
  tourContentId: string | null
  tourContentTypeId: string | null
}

export type CourseListItem = {
  id: string
  name: string
  description: string | null
  courseType: CourseType
  distanceKm: number
  estimatedDurationMinutes: number
  elevationGainM: number | null
  difficulty: CourseDifficulty
  thumbnailImageUrl: string | null
  isPublic: boolean
  ratingAvg: number
  completionCount: number
  createdByMe: boolean
  bookmarkedByMe: boolean
  bookmarkId: string | null
  createdAt: string
  waypointNames: string[]
  previewRouteCoordinates: CourseRouteCoordinate[]
  waypoints: CourseWaypoint[]
}

export type CourseDetail = Omit<CourseListItem, 'previewRouteCoordinates' | 'waypointNames'> & {
  surfaceAsphaltPct: number
  surfaceDirtPct: number
  surfaceStairsPct: number
  routeCoordinates: CourseRouteCoordinate[]
}

export type CourseBookmarkResponse = {
  bookmarkId: string
}
