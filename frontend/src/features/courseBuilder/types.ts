export type JsonPrimitive = string | number | boolean | null
export type JsonValue = JsonPrimitive | JsonValue[] | { [key: string]: JsonValue }

export type LatLng = {
  lat: number
  lng: number
}

export type PlaceSearchResult = {
  kakaoPlaceId: string
  name: string
  categoryGroupCode: string | null
  categoryName: string | null
  address: string | null
  lat: number
  lng: number
  isTourismCandidate: boolean
}

export type NearbyCategoryGroupCode = 'AT4' | 'CE7' | 'FD6' | 'CS2' | 'AD5'

export type PlaceDetail = {
  kakaoPlaceId: string
  name: string
  categoryName: string | null
  address: string | null
  lat: number
  lng: number
  phone: string | null
  kakaoPlaceUrl: string | null
  tourApiMatched: boolean
  tourContentId: string | null
  tourContentTypeId: string | null
  overview: string | null
  firstImageUrl: string | null
  useTime: string | null
  tourDataRaw: JsonValue | null
}

export type CourseType = 'RUNNING_COURSE' | 'SPOT_COURSE'

export type ThemeOption = {
  id: string
  code: string
  name: string
}

export type CourseTagOption = {
  id: string
  name: string
}

export type CourseWaypointDraft = {
  kakaoPlaceId: string | null
  name: string
  categoryGroupCode: string | null
  categoryName: string | null
  address: string | null
  phone: string | null
  lat: number
  lng: number
  orderIndex: number
  tourContentId: string | null
  tourContentTypeId: string | null
  firstImageUrl: string | null
  tourDataRaw: JsonValue | null
}

export type RouteSurface = {
  asphaltPct: number
  dirtPct: number
  stairsPct: number
}

export type RouteCoordinate = {
  lat: number
  lng: number
}

export type DraftRoute = {
  distanceKm: number
  estimatedDurationMinutes: number
  elevationGainM: number
  surface: RouteSurface | null
  routeCoordinates: RouteCoordinate[]
  routeLineStringWkt: string
  suggestedDifficulty: 'LOW' | 'MID' | 'HIGH'
}

export type CourseCreateResponse = {
  courseId: string
}
