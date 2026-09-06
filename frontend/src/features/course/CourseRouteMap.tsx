import { useEffect, useRef, useState } from 'react'
import { getKakaoMapAppKey, loadKakaoMapSdk } from '../map/kakaoMaps'
import type { KakaoCustomOverlay, KakaoMap, KakaoPolyline } from '../map/kakaoMaps'
import type { CourseRouteCoordinate, CourseWaypoint } from './types'

type CourseRouteMapFitTarget = 'all' | 'planned' | 'recorded'
type CourseRouteLineStyle = {
  strokeWeight?: number
  strokeColor?: string
  strokeOpacity?: number
  strokeStyle?: string
}

type CourseRouteMapProps = {
  routeCoordinates: CourseRouteCoordinate[]
  waypoints: CourseWaypoint[]
  currentPosition?: CourseRouteCoordinate | null
  recordedPath?: CourseRouteCoordinate[]
  fitTarget?: CourseRouteMapFitTarget
  plannedRouteStyle?: CourseRouteLineStyle
  recordedRouteStyle?: CourseRouteLineStyle
  showCurrentPositionMarker?: boolean
  className?: string
  showZoomControls?: boolean
}

const JEJU_CENTER = { lat: 33.3846, lng: 126.5535 }

export function CourseRouteMap({
  routeCoordinates,
  waypoints,
  currentPosition = null,
  recordedPath = [],
  fitTarget = 'all',
  plannedRouteStyle,
  recordedRouteStyle,
  showCurrentPositionMarker = true,
  className = '',
  showZoomControls = false,
}: CourseRouteMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<KakaoMap | null>(null)
  const plannedRouteRef = useRef<KakaoPolyline | null>(null)
  const recordedRouteRef = useRef<KakaoPolyline | null>(null)
  const waypointOverlayRefs = useRef<KakaoCustomOverlay[]>([])
  const currentOverlayRef = useRef<KakaoCustomOverlay | null>(null)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const appKey = getKakaoMapAppKey()

  useEffect(() => {
    if (!containerRef.current || !appKey) return
    let disposed = false

    loadKakaoMapSdk(appKey)
      .then(() => {
        if (disposed || !containerRef.current || !window.kakao) return
        const maps = window.kakao.maps
        const center = getFitPoints(fitTarget, routeCoordinates, recordedPath, waypoints)[0]
          ?? currentPosition
          ?? JEJU_CENTER
        const map = new maps.Map(containerRef.current, {
          center: new maps.LatLng(center.lat, center.lng),
          level: 5,
        })
        plannedRouteRef.current = new maps.Polyline({
          map,
          path: [],
          strokeWeight: plannedRouteStyle?.strokeWeight ?? 6,
          strokeColor: plannedRouteStyle?.strokeColor ?? '#FF6F0F',
          strokeOpacity: plannedRouteStyle?.strokeOpacity ?? 0.96,
          strokeStyle: plannedRouteStyle?.strokeStyle ?? 'solid',
        })
        recordedRouteRef.current = new maps.Polyline({
          map,
          path: [],
          strokeWeight: recordedRouteStyle?.strokeWeight ?? 5,
          strokeColor: recordedRouteStyle?.strokeColor ?? '#1D9A45',
          strokeOpacity: recordedRouteStyle?.strokeOpacity ?? 0.85,
          strokeStyle: recordedRouteStyle?.strokeStyle ?? 'solid',
        })
        mapRef.current = map
        setReady(true)
        window.setTimeout(() => map.relayout(), 0)
      })
      .catch((reason: Error) => setError(reason.message))

    return () => {
      disposed = true
    }
  // The map instance should be created once; overlays update in dedicated effects.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appKey])

  useEffect(() => {
    if (!ready || !plannedRouteRef.current || !window.kakao) return
    plannedRouteRef.current.setPath(toKakaoPath(routeCoordinates))
  }, [ready, routeCoordinates])

  useEffect(() => {
    if (!ready || !recordedRouteRef.current || !window.kakao) return
    recordedRouteRef.current.setPath(toKakaoPath(recordedPath))
  }, [ready, recordedPath])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao) return
    waypointOverlayRefs.current.forEach((overlay) => overlay.setMap(null))
    waypointOverlayRefs.current = waypoints.map((waypoint, index) => (
      new window.kakao!.maps.CustomOverlay({
        map: mapRef.current!,
        position: new window.kakao!.maps.LatLng(waypoint.lat, waypoint.lng),
        content: `<div class="course-route-map-marker">${index + 1}</div>`,
        zIndex: 12,
      })
    ))
  }, [ready, waypoints])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao || !showCurrentPositionMarker || !currentPosition) {
      currentOverlayRef.current?.setMap(null)
      currentOverlayRef.current = null
      return
    }
    const position = new window.kakao.maps.LatLng(currentPosition.lat, currentPosition.lng)
    if (!currentOverlayRef.current) {
      currentOverlayRef.current = new window.kakao.maps.CustomOverlay({
        map: mapRef.current,
        position,
        content: '<div class="course-route-map-current"><span></span></div>',
        zIndex: 14,
      })
    } else {
      currentOverlayRef.current.setPosition(position)
    }
  }, [currentPosition, ready, showCurrentPositionMarker])

  useEffect(() => {
    if (!ready || !mapRef.current || !window.kakao) return
    const points = getFitPoints(fitTarget, routeCoordinates, recordedPath, waypoints)
    if (points.length === 0) return
    if (points.length === 1) {
      mapRef.current.setCenter(new window.kakao.maps.LatLng(points[0].lat, points[0].lng))
      return
    }
    const bounds = new window.kakao.maps.LatLngBounds()
    points.forEach((point) => bounds.extend(new window.kakao!.maps.LatLng(point.lat, point.lng)))
    window.setTimeout(() => mapRef.current?.setBounds(bounds), 0)
  }, [fitTarget, ready, recordedPath, routeCoordinates, waypoints])

  function zoomBy(delta: number) {
    if (!mapRef.current) return
    const nextLevel = Math.max(1, Math.min(14, mapRef.current.getLevel() + delta))
    mapRef.current.setLevel(nextLevel)
  }

  if (!appKey) {
    return <div className={`course-route-map-fallback ${className}`}>카카오맵 JavaScript 키를 설정해 주세요.</div>
  }

  return (
    <div className={`course-route-map ${className}`}>
      <div ref={containerRef} className="course-route-map-canvas" />
      {error && (
        <div className="course-route-map-fallback">
          {error}
          <br />
          JavaScript 키와 등록 도메인을 확인해 주세요.
        </div>
      )}
      {showZoomControls && (
        <div className="course-route-map-zoom" aria-label="지도 확대/축소">
          <button type="button" aria-label="지도 확대" onClick={() => zoomBy(-1)}>+</button>
          <button type="button" aria-label="지도 축소" onClick={() => zoomBy(1)}>-</button>
        </div>
      )}
    </div>
  )
}

function toKakaoPath(coordinates: CourseRouteCoordinate[]) {
  if (!window.kakao) return []
  return coordinates.map((coordinate) => new window.kakao!.maps.LatLng(coordinate.lat, coordinate.lng))
}

function getFitPoints(
  fitTarget: CourseRouteMapFitTarget,
  routeCoordinates: CourseRouteCoordinate[],
  recordedPath: CourseRouteCoordinate[],
  waypoints: CourseWaypoint[],
) {
  const plannedPoints = [...routeCoordinates, ...waypoints.map(({ lat, lng }) => ({ lat, lng }))]
  if (fitTarget === 'planned') {
    return plannedPoints.length > 0 ? plannedPoints : recordedPath
  }
  if (fitTarget === 'recorded') {
    return recordedPath.length > 0 ? recordedPath : plannedPoints
  }
  return [...plannedPoints, ...recordedPath]
}
