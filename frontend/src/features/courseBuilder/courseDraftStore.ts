import { create } from 'zustand'
import type { CourseWaypointDraft, DraftRoute, PlaceDetail, PlaceSearchResult } from './types'

let waypointDraftSequence = 0

type CourseDraftState = {
  waypoints: CourseWaypointDraft[]
  selectedPlace: PlaceSearchResult | null
  selectedPlaceDetail: PlaceDetail | null
  draftRoute: DraftRoute | null
  routeStatus: 'idle' | 'loading' | 'success' | 'error'
  routeError: string | null
  setSelectedPlace: (place: PlaceSearchResult | null) => void
  setSelectedPlaceDetail: (detail: PlaceDetail | null) => void
  addWaypoint: (place: PlaceSearchResult, detail: PlaceDetail) => void
  removeWaypoint: (orderIndex: number) => void
  moveWaypoint: (fromIndex: number, toIndex: number) => void
  setDraftRoute: (draftRoute: DraftRoute | null) => void
  setRouteStatus: (status: CourseDraftState['routeStatus'], message?: string | null) => void
  resetDraft: () => void
}

function toWaypoint(place: PlaceSearchResult, detail: PlaceDetail, orderIndex: number): CourseWaypointDraft {
  waypointDraftSequence += 1
  return {
    draftId: `${detail.kakaoPlaceId || place.kakaoPlaceId}-${Date.now()}-${waypointDraftSequence}`,
    kakaoPlaceId: detail.kakaoPlaceId || place.kakaoPlaceId,
    name: detail.name || place.name,
    categoryGroupCode: place.categoryGroupCode,
    categoryName: detail.categoryName || place.categoryName,
    address: detail.address || place.address,
    phone: detail.phone,
    lat: detail.lat,
    lng: detail.lng,
    orderIndex,
    tourContentId: detail.tourContentId,
    tourContentTypeId: detail.tourContentTypeId,
    firstImageUrl: detail.firstImageUrl,
    tourDataRaw: detail.tourDataRaw,
  }
}

function reorderWaypoints(waypoints: CourseWaypointDraft[]) {
  return waypoints.map((waypoint, index) => ({ ...waypoint, orderIndex: index }))
}

function isSameWaypoint(left: CourseWaypointDraft | undefined, right: CourseWaypointDraft | undefined) {
  if (!left || !right) return false
  if (left.kakaoPlaceId && right.kakaoPlaceId) return left.kakaoPlaceId === right.kakaoPlaceId
  return left.lat === right.lat && left.lng === right.lng
}

function withoutConsecutiveDuplicates(waypoints: CourseWaypointDraft[]) {
  return waypoints.filter((waypoint, index) => !isSameWaypoint(waypoints[index - 1], waypoint))
}

export const useCourseDraftStore = create<CourseDraftState>((set) => ({
  waypoints: [],
  selectedPlace: null,
  selectedPlaceDetail: null,
  draftRoute: null,
  routeStatus: 'idle',
  routeError: null,

  setSelectedPlace: (place) => set({ selectedPlace: place }),
  setSelectedPlaceDetail: (detail) => set({ selectedPlaceDetail: detail }),
  addWaypoint: (place, detail) => set((state) => {
    const previousWaypoint = state.waypoints.at(-1)
    const nextPlaceId = detail.kakaoPlaceId || place.kakaoPlaceId
    if (previousWaypoint?.kakaoPlaceId === nextPlaceId) return state

    return {
      waypoints: [...state.waypoints, toWaypoint(place, detail, state.waypoints.length)],
      selectedPlace: null,
      selectedPlaceDetail: null,
    }
  }),
  removeWaypoint: (orderIndex) => set((state) => ({
    waypoints: reorderWaypoints(withoutConsecutiveDuplicates(
      state.waypoints.filter((waypoint) => waypoint.orderIndex !== orderIndex),
    )),
  })),
  moveWaypoint: (fromIndex, toIndex) => set((state) => {
    if (fromIndex === toIndex
      || fromIndex < 0
      || toIndex < 0
      || fromIndex >= state.waypoints.length
      || toIndex >= state.waypoints.length) {
      return state
    }

    const nextWaypoints = [...state.waypoints]
    const [movedWaypoint] = nextWaypoints.splice(fromIndex, 1)
    nextWaypoints.splice(toIndex, 0, movedWaypoint)
    if (nextWaypoints.some((waypoint, index) => isSameWaypoint(nextWaypoints[index - 1], waypoint))) {
      return state
    }
    return { waypoints: reorderWaypoints(nextWaypoints) }
  }),
  setDraftRoute: (draftRoute) => set({ draftRoute }),
  setRouteStatus: (routeStatus, routeError = null) => set({ routeStatus, routeError }),
  resetDraft: () => set({
    waypoints: [],
    selectedPlace: null,
    selectedPlaceDetail: null,
    draftRoute: null,
    routeStatus: 'idle',
    routeError: null,
  }),
}))
