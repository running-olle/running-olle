import { axiosInstance } from '../../api/axiosInstance'
import type { CourseCreateResponse, CourseTagOption, CourseType, CourseWaypointDraft, DraftRoute, NearbyCategoryGroupCode, PlaceDetail, PlaceSearchResult, ThemeOption } from './types'

type DraftRouteWaypointRequest = {
  kakaoPlaceId: string | null
  name: string
  lat: number
  lng: number
  orderIndex: number
}

type CourseCreateWaypointRequest = {
  kakaoPlaceId: string | null
  name: string
  lat: number
  lng: number
  orderIndex: number
  description: string | null
  tourContentId: string | null
  tourContentTypeId: string | null
  firstImageUrl: string | null
  thumbnailImageUrl: string | null
  tourDataRaw: CourseWaypointDraft['tourDataRaw']
}

type CourseCreateRequest = {
  name: string
  description: string | null
  courseType: CourseType
  waypoints: CourseCreateWaypointRequest[]
  themeIds: string[]
  tagIds: string[]
  isPublic: boolean
}

export const courseBuilderService = {
  getThemes() {
    return axiosInstance.get<ThemeOption[]>('/themes').then(({ data }) => data)
  },

  getCourseTags() {
    return axiosInstance.get<CourseTagOption[]>('/course-tags').then(({ data }) => data)
  },

  searchPlaces(keyword: string, lat: number, lng: number, radius: number) {
    return axiosInstance.get<PlaceSearchResult[]>('/places/search', {
      params: { keyword, lat, lng, radius },
    }).then(({ data }) => data)
  },

  searchNearbyPlaces(lat: number, lng: number, radius: number, categoryGroupCode: NearbyCategoryGroupCode) {
    return axiosInstance.get<PlaceSearchResult[]>('/places/nearby', {
      params: { lat, lng, radius, categoryGroupCode },
    }).then(({ data }) => data)
  },

  getPlaceDetail(place: PlaceSearchResult) {
    return axiosInstance.get<PlaceDetail>(`/places/${place.kakaoPlaceId}/detail`, {
      params: {
        name: place.name,
        lat: place.lat,
        lng: place.lng,
        categoryGroupCode: place.categoryGroupCode,
      },
    }).then(({ data }) => data)
  },

  calculateDraftRoute(waypoints: CourseWaypointDraft[]) {
    const body: { waypoints: DraftRouteWaypointRequest[] } = {
      waypoints: waypoints.map((waypoint, index) => ({
        kakaoPlaceId: waypoint.kakaoPlaceId,
        name: waypoint.name,
        lat: waypoint.lat,
        lng: waypoint.lng,
        orderIndex: index,
      })),
    }

    return axiosInstance.post<DraftRoute>('/courses/draft/route', body).then(({ data }) => data)
  },

  createCourse(params: {
    name: string
    description: string | null
    courseType: CourseType
    waypoints: CourseWaypointDraft[]
    themeIds: string[]
    tagIds: string[]
    isPublic: boolean
  }) {
    const body: CourseCreateRequest = {
      name: params.name,
      description: params.description,
      courseType: params.courseType,
      themeIds: params.themeIds,
      tagIds: params.tagIds,
      isPublic: params.isPublic,
      waypoints: params.waypoints.map((waypoint, index) => ({
        kakaoPlaceId: waypoint.kakaoPlaceId,
        name: waypoint.name,
        lat: waypoint.lat,
        lng: waypoint.lng,
        orderIndex: index,
        description: waypoint.address,
        tourContentId: waypoint.tourContentId,
        tourContentTypeId: waypoint.tourContentTypeId,
        firstImageUrl: waypoint.firstImageUrl,
        thumbnailImageUrl: waypoint.firstImageUrl,
        tourDataRaw: waypoint.tourDataRaw,
      })),
    }

    return axiosInstance.post<CourseCreateResponse>('/courses', body).then(({ data }) => data)
  },
}
