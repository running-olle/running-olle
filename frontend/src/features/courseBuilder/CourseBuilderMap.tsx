import { useEffect, useRef, useState } from 'react'
import { JEJU_CENTER } from './courseBuilderUtils'
import { getKakaoMapAppKey, loadKakaoMapSdk } from '../map/kakaoMaps'
import type { KakaoCustomOverlay, KakaoMap, KakaoPolyline } from '../map/kakaoMaps'
import type { CourseWaypointDraft, DraftRoute, LatLng, PlaceSearchResult } from './types'

type Props = {
  currentPosition: LatLng | null
  waypoints: CourseWaypointDraft[]
  draftRoute: DraftRoute | null
  selectedPlace: PlaceSearchResult | null
  searchAnchorPlace?: PlaceSearchResult | null
  candidatePlaces?: PlaceSearchResult[]
  className?: string
  onMapPress?: () => void
  onSelectedPlaceMarkerClick?: () => void
  onCandidatePlaceClick?: (place: PlaceSearchResult) => void
}

function markerContent(index: number) {
  return `<div class="course-builder-marker">${index}</div>`
}

function selectedMarkerContent(onClick?: () => void) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'course-builder-selected-marker'
  button.setAttribute('aria-label', '선택한 장소 상세 보기')
  button.innerHTML = '<span></span>'
  button.addEventListener('click', (event) => {
    event.stopPropagation()
    onClick?.()
  })
  return button
}

function currentMarkerContent() {
  return '<div class="course-builder-current-marker"><span></span></div>'
}

function candidateMarkerClass(categoryGroupCode: string | null) {
  if (categoryGroupCode === 'AT4') return 'is-tourism'
  if (categoryGroupCode === 'CE7') return 'is-cafe'
  if (categoryGroupCode === 'FD6') return 'is-food'
  if (categoryGroupCode === 'CS2') return 'is-store'
  if (categoryGroupCode === 'AD5') return 'is-stay'
  return 'is-place'
}

function candidateMarkerIcon(categoryGroupCode: string | null) {
  if (categoryGroupCode === 'AT4') {
    return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z" /></svg>'
  }
  if (categoryGroupCode === 'CE7') {
    return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 8h11v5a5 5 0 0 1-5 5H10a5 5 0 0 1-5-5V8Zm11 2h2.3a2.7 2.7 0 1 1 0 5.4H16" /></svg>'
  }
  if (categoryGroupCode === 'FD6') {
    return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 3v8M10 3v8M5 11h7M8.5 11v10M17 3v18M15 3c3 2.5 3 5.5 0 8" /></svg>'
  }
  if (categoryGroupCode === 'CS2') {
    return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 9h16l-1-5H5L4 9Zm1 0v11h14V9M8 20v-7h8v7" /></svg>'
  }
  if (categoryGroupCode === 'AD5') {
    return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 11h16v8M4 19V7M20 19v-5a3 3 0 0 0-3-3H4M8 11V8h4a2 2 0 0 1 2 2v1" /></svg>'
  }
  return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5h10v14H7V5Zm2 4h6M9 13h6" /></svg>'
}

function candidateMarkerContent(place: PlaceSearchResult, onClick?: (place: PlaceSearchResult) => void) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = `course-builder-candidate-marker ${candidateMarkerClass(place.categoryGroupCode)}`
  button.setAttribute('aria-label', `${place.name} 상세 보기`)
  button.innerHTML = candidateMarkerIcon(place.categoryGroupCode)
  button.addEventListener('click', (event) => {
    event.stopPropagation()
    onClick?.(place)
  })
  return button
}

export function CourseBuilderMap({
  currentPosition,
  waypoints,
  draftRoute,
  selectedPlace,
  searchAnchorPlace,
  candidatePlaces = [],
  className = '',
  onMapPress,
  onSelectedPlaceMarkerClick,
  onCandidatePlaceClick,
}: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<KakaoMap | null>(null)
  const routeRef = useRef<KakaoPolyline | null>(null)
  const waypointOverlayRefs = useRef<KakaoCustomOverlay[]>([])
  const candidateOverlayRefs = useRef<KakaoCustomOverlay[]>([])
  const anchorOverlayRef = useRef<KakaoCustomOverlay | null>(null)
  const selectedOverlayRef = useRef<KakaoCustomOverlay | null>(null)
  const currentOverlayRef = useRef<KakaoCustomOverlay | null>(null)
  const onMapPressRef = useRef(onMapPress)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const appKey = getKakaoMapAppKey()

  useEffect(() => {
    onMapPressRef.current = onMapPress
  }, [onMapPress])

  useEffect(() => {
    if (!containerRef.current || !appKey) return
    let disposed = false

    loadKakaoMapSdk(appKey)
      .then(() => {
        if (disposed || !containerRef.current || !window.kakao) return
        const maps = window.kakao.maps
        const center = JEJU_CENTER
        const map = new maps.Map(containerRef.current, {
          center: new maps.LatLng(center.lat, center.lng),
          level: 6,
        })
        routeRef.current = new maps.Polyline({
          map,
          path: [],
          strokeWeight: 5,
          strokeColor: '#FF6F0F',
          strokeOpacity: 0.96,
          strokeStyle: 'solid',
        })
        maps.event.addListener(map, 'click', () => onMapPressRef.current?.())
        mapRef.current = map
        setReady(true)
        window.setTimeout(() => map.relayout(), 0)
      })
      .catch((reason: Error) => setError(reason.message))

    return () => {
      disposed = true
    }
  }, [appKey])

  useEffect(() => {
    if (!ready || !mapRef.current || !currentPosition || !window.kakao) return
    const position = new window.kakao.maps.LatLng(currentPosition.lat, currentPosition.lng)
    if (!currentOverlayRef.current) {
      currentOverlayRef.current = new window.kakao.maps.CustomOverlay({
        map: mapRef.current,
        position,
        content: currentMarkerContent(),
        zIndex: 8,
      })
    } else {
      currentOverlayRef.current.setPosition(position)
    }
  }, [currentPosition, ready])

  useEffect(() => {
    if (!ready || !routeRef.current || !window.kakao) return
    const path = draftRoute?.routeCoordinates.map((coordinate) => (
      new window.kakao!.maps.LatLng(coordinate.lat, coordinate.lng)
    )) ?? []
    routeRef.current.setPath(path)
  }, [draftRoute, ready])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao) return
    waypointOverlayRefs.current.forEach((overlay) => overlay.setMap(null))
    waypointOverlayRefs.current = waypoints.map((waypoint, index) => (
      new window.kakao!.maps.CustomOverlay({
        map: mapRef.current!,
        position: new window.kakao!.maps.LatLng(waypoint.lat, waypoint.lng),
        content: markerContent(index + 1),
        zIndex: 12,
      })
    ))
  }, [ready, waypoints])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao) return
    candidateOverlayRefs.current.forEach((overlay) => overlay.setMap(null))
    candidateOverlayRefs.current = candidatePlaces.map((place) => (
      new window.kakao!.maps.CustomOverlay({
        map: mapRef.current!,
        position: new window.kakao!.maps.LatLng(place.lat, place.lng),
        content: candidateMarkerContent(place, onCandidatePlaceClick),
        zIndex: 9,
        yAnchor: 1,
      })
    ))
  }, [candidatePlaces, onCandidatePlaceClick, ready])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao) return
    anchorOverlayRef.current?.setMap(null)
    anchorOverlayRef.current = null
    if (!searchAnchorPlace || searchAnchorPlace.kakaoPlaceId === selectedPlace?.kakaoPlaceId) return

    anchorOverlayRef.current = new window.kakao.maps.CustomOverlay({
      map: mapRef.current,
      position: new window.kakao.maps.LatLng(searchAnchorPlace.lat, searchAnchorPlace.lng),
      content: candidateMarkerContent(searchAnchorPlace, onCandidatePlaceClick),
      zIndex: 10,
      yAnchor: 1,
    })
  }, [onCandidatePlaceClick, ready, searchAnchorPlace, selectedPlace?.kakaoPlaceId])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao) return
    selectedOverlayRef.current?.setMap(null)
    selectedOverlayRef.current = null
    if (!selectedPlace) return

    const position = new window.kakao.maps.LatLng(selectedPlace.lat, selectedPlace.lng)
    selectedOverlayRef.current = new window.kakao.maps.CustomOverlay({
      map: mapRef.current,
      position,
      content: selectedMarkerContent(onSelectedPlaceMarkerClick),
      zIndex: 11,
      yAnchor: 1,
    })
    mapRef.current.panTo(position)
  }, [onSelectedPlaceMarkerClick, ready, selectedPlace])

  function zoomBy(delta: number) {
    if (!mapRef.current) return
    const nextLevel = Math.max(1, Math.min(14, mapRef.current.getLevel() + delta))
    mapRef.current.setLevel(nextLevel)
  }

  if (!appKey) {
    return (
      <div className={`course-builder-map-fallback ${className}`}>
        카카오맵 JavaScript 키를 설정해 주세요.
      </div>
    )
  }

  return (
    <div className={`course-builder-map ${className}`}>
      <div ref={containerRef} className="h-full w-full" />
      {error && (
        <div className="course-builder-map-fallback">
          {error}
          <br />
          JavaScript 키와 등록 도메인을 확인해 주세요.
        </div>
      )}
      <div className="course-builder-zoom-controls" aria-label="지도 확대/축소">
        <button type="button" aria-label="지도 확대" onClick={() => zoomBy(-1)}>+</button>
        <button type="button" aria-label="지도 축소" onClick={() => zoomBy(1)}>-</button>
      </div>
    </div>
  )
}
