import { useEffect, useRef, useState } from 'react'
import type { Course, GeoPoint, RouteProgress } from '../models/running'

type RunningMapProps = {
  course: Course
  currentPosition: GeoPoint | null
  recordedPath: GeoPoint[]
  progress: RouteProgress
  followPosition: boolean
}

let kakaoMapLoader: Promise<void> | null = null

function loadKakaoMapSdk(appKey: string) {
  if (window.kakao?.maps) return Promise.resolve()
  if (kakaoMapLoader) return kakaoMapLoader

  kakaoMapLoader = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false`
    script.async = true
    script.onload = () => {
      if (!window.kakao?.maps) {
        reject(new Error('카카오맵 SDK를 불러오지 못했습니다.'))
        return
      }
      window.kakao.maps.load(resolve)
    }
    script.onerror = () => reject(new Error('카카오맵 SDK 요청에 실패했습니다.'))
    document.head.appendChild(script)
  })

  return kakaoMapLoader
}

function RunningMap({ course, currentPosition, recordedPath, progress, followPosition }: RunningMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<any>(null)
  const currentMarkerRef = useRef<any>(null)
  const accuracyCircleRef = useRef<any>(null)
  const recordedPolylineRef = useRef<any>(null)
  const progressPolylineRef = useRef<any>(null)
  const [mapError, setMapError] = useState<string | null>(null)
  const [mapReady, setMapReady] = useState(false)
  const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY

  useEffect(() => {
    if (!containerRef.current || !appKey) return
    let disposed = false

    loadKakaoMapSdk(appKey)
      .then(() => {
        if (disposed || !containerRef.current || !window.kakao) return

        const { maps } = window.kakao
        const map = new maps.Map(containerRef.current, {
          center: new maps.LatLng(course.path[0].latitude, course.path[0].longitude),
          level: 4,
        })
        map.addControl(new maps.ZoomControl(), maps.ControlPosition.RIGHT)

        const routePath = course.path.map((point) => new maps.LatLng(point.latitude, point.longitude))
        new maps.Polyline({
          map,
          path: routePath,
          strokeWeight: 8,
          strokeColor: '#cad4d0',
          strokeOpacity: 0.95,
          strokeStyle: 'solid',
        })
        progressPolylineRef.current = new maps.Polyline({
          map,
          path: [],
          strokeWeight: 8,
          strokeColor: '#ff6b35',
          strokeOpacity: 1,
          strokeStyle: 'solid',
        })
        recordedPolylineRef.current = new maps.Polyline({
          map,
          path: [],
          strokeWeight: 5,
          strokeColor: '#007f73',
          strokeOpacity: 0.9,
          strokeStyle: 'solid',
        })

        const bounds = new maps.LatLngBounds()
        routePath.forEach((point) => bounds.extend(point))
        map.setBounds(bounds, 34, 34, 34, 34)
        new maps.Marker({ map, position: routePath[0], title: '출발' })
        new maps.Marker({ map, position: routePath[routePath.length - 1], title: '도착' })
        mapRef.current = map
        setMapReady(true)
        window.setTimeout(() => map.relayout(), 0)
      })
      .catch((error: Error) => setMapError(error.message))

    return () => {
      disposed = true
    }
  }, [appKey, course])

  useEffect(() => {
    if (!mapRef.current || !window.kakao || !currentPosition) return
    const { maps } = window.kakao
    const position = new maps.LatLng(currentPosition.latitude, currentPosition.longitude)

    if (!currentMarkerRef.current) {
      currentMarkerRef.current = new maps.CustomOverlay({
        map: mapRef.current,
        position,
        content: '<div class="current-location-marker"><span></span></div>',
        zIndex: 10,
      })
    } else {
      currentMarkerRef.current.setPosition(position)
    }

    if (!accuracyCircleRef.current) {
      accuracyCircleRef.current = new maps.Circle({
        map: mapRef.current,
        center: position,
        radius: currentPosition.accuracy ?? 0,
        strokeWeight: 1,
        strokeColor: '#1769e0',
        strokeOpacity: 0.4,
        fillColor: '#80b4ff',
        fillOpacity: 0.15,
      })
    } else {
      accuracyCircleRef.current.setPosition(position)
      accuracyCircleRef.current.setRadius(currentPosition.accuracy ?? 0)
    }

    if (followPosition) mapRef.current.panTo(position)
  }, [currentPosition, followPosition, mapReady])

  useEffect(() => {
    if (!window.kakao || !recordedPolylineRef.current) return
    recordedPolylineRef.current.setPath(
      recordedPath.map((point) => new window.kakao!.maps.LatLng(point.latitude, point.longitude)),
    )
  }, [recordedPath, mapReady])

  useEffect(() => {
    if (!window.kakao || !progressPolylineRef.current) return
    progressPolylineRef.current.setPath(
      progress.completedPath.map((point) => new window.kakao!.maps.LatLng(point.latitude, point.longitude)),
    )
  }, [progress, mapReady])

  if (!appKey) return <div className="map-fallback">카카오맵 API 키를 설정하면 지도가 표시됩니다.</div>

  return (
    <>
      <div ref={containerRef} className="map-container" aria-label={`${course.name} 지도`} />
      {mapError && <div className="map-fallback">{mapError}<br />등록 도메인과 JavaScript 키를 확인해 주세요.</div>}
    </>
  )
}

export default RunningMap
