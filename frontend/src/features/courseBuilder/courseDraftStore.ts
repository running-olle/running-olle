import { create } from 'zustand'
import type { CourseWaypointDraft, DraftRoute, PlaceDetail, PlaceSearchResult } from './types'

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
  setDraftRoute: (draftRoute: DraftRoute | null) => void
  setRouteStatus: (status: CourseDraftState['routeStatus'], message?: string | null) => void
  resetDraft: () => void
}

function toWaypoint(place: PlaceSearchResult, detail: PlaceDetail, orderIndex: number): CourseWaypointDraft {
  return {
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

export const useCourseDraftStore = create<CourseDraftState>((set) => ({
  waypoints: [],
  selectedPlace: null,
  selectedPlaceDetail: null,
  draftRoute: null,
  routeStatus: 'idle',
  routeError: null,

  setSelectedPlace: (place) => set({ selectedPlace: place }),
  setSelectedPlaceDetail: (detail) => set({ selectedPlaceDetail: detail }),
  addWaypoint: (place, detail) => set((state) => ({
    waypoints: [...state.waypoints, toWaypoint(place, detail, state.waypoints.length)],
    selectedPlace: null,
    selectedPlaceDetail: null,
  })),
  removeWaypoint: (orderIndex) => set((state) => ({
    waypoints: reorderWaypoints(state.waypoints.filter((waypoint) => waypoint.orderIndex !== orderIndex)),
  })),
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
