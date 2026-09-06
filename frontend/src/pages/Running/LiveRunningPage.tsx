import { useEffect, useMemo, useRef, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { CourseRouteMap } from '../../features/course/CourseRouteMap'
import { courseService } from '../../features/course/courseService'
import type { CourseDetail } from '../../features/course/types'
import { FreeRunningMap } from '../../features/running/FreeRunningMap'
import { RunningIcon } from '../../features/running/RunningIcon'
import { saveRunningRecord } from '../../features/running/runningRecordService'
import { distanceBetween, formatDistance, formatDuration, formatPace, getLocationErrorMessage, GPS_OPTIONS, positionToPoint } from '../../features/running/runningUtils'
import type { GeoPoint, RunningMode, RunningPhase } from '../../features/running/types'

const MIN_SEGMENT_METERS = 3
const MAX_SEGMENT_METERS = 200
const MAX_ACCURACY_METERS = 80

type RunningNavigationState = {
  startPosition?: GeoPoint
  courseId?: string
  courseName?: string
  runningMode: RunningMode
}

export function LiveRunningPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const navigationState = readRunningNavigationState(location.state as unknown)
  const startPosition = navigationState?.startPosition
  const [phase, setPhase] = useState<RunningPhase>('running')
  const [position, setPosition] = useState<GeoPoint | null>(startPosition ?? null)
  const [courseDetail, setCourseDetail] = useState<CourseDetail | null>(null)
  const [route, setRoute] = useState<GeoPoint[]>(startPosition ? [startPosition] : [])
  const [distanceMeters, setDistanceMeters] = useState(0)
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [locked, setLocked] = useState(false)
  const [showEndSheet, setShowEndSheet] = useState(false)
  const [saving, setSaving] = useState(false)
  const [locationError, setLocationError] = useState<string | null>(null)
  const [photoCount, setPhotoCount] = useState(0)
  const lastPositionRef = useRef<GeoPoint | null>(startPosition ?? null)
  const startedAtRef = useRef(new Date())
  const activeElapsedMsRef = useRef(0)
  const phaseStartedAtRef = useRef(Date.now())
  const cameraInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!navigationState?.courseId) {
      setCourseDetail(null)
      return
    }
    let ignore = false
    courseService.getCourse(navigationState.courseId)
      .then((detail) => {
        if (!ignore) setCourseDetail(detail)
      })
      .catch(() => {
        if (!ignore) setCourseDetail(null)
      })
    return () => {
      ignore = true
    }
  }, [navigationState?.courseId])

  useEffect(() => {
    if (phase !== 'running') return
    const updateElapsed = () => setElapsedSeconds(Math.floor(
      (activeElapsedMsRef.current + Date.now() - phaseStartedAtRef.current) / 1_000,
    ))
    updateElapsed()
    const timer = window.setInterval(updateElapsed, 500)
    return () => window.clearInterval(timer)
  }, [phase])

  useEffect(() => {
    if (phase !== 'running' || !navigator.geolocation) return
    const watchId = navigator.geolocation.watchPosition((result) => {
      const next = positionToPoint(result)
      setPosition(next)
      setLocationError(null)
      if ((next.accuracy ?? 0) > MAX_ACCURACY_METERS) return
      const previous = lastPositionRef.current
      if (!previous) {
        lastPositionRef.current = next
        setRoute((value) => [...value, next])
        return
      }
      const segment = distanceBetween(previous, next)
      if (segment < MIN_SEGMENT_METERS || segment > MAX_SEGMENT_METERS) return
      lastPositionRef.current = next
      setDistanceMeters((value) => value + segment)
      setRoute((value) => [...value, next])
    }, (error) => setLocationError(getLocationErrorMessage(error)), GPS_OPTIONS)
    return () => navigator.geolocation.clearWatch(watchId)
  }, [phase])

  const averagePace = useMemo(
    () => distanceMeters >= 10 ? elapsedSeconds / 60 / (distanceMeters / 1_000) : null,
    [distanceMeters, elapsedSeconds],
  )

  const togglePause = () => {
    lastPositionRef.current = null
    if (phase === 'running') {
      activeElapsedMsRef.current += Date.now() - phaseStartedAtRef.current
      setElapsedSeconds(Math.floor(activeElapsedMsRef.current / 1_000))
      setPhase('paused')
    } else {
      phaseStartedAtRef.current = Date.now()
      setPhase('running')
    }
  }

  const finishRun = async () => {
    setSaving(true)
    const finalRoute = route.length > 0 ? route : position ? [position] : []
    const finalDuration = Math.floor((activeElapsedMsRef.current
      + (phase === 'running' ? Date.now() - phaseStartedAtRef.current : 0)) / 1_000)
    const finalAveragePace = distanceMeters >= 10
      ? finalDuration / 60 / (distanceMeters / 1_000)
      : null
    const record = await saveRunningRecord({
      courseId: navigationState?.courseId ?? null,
      courseName: navigationState?.courseName ?? courseDetail?.name ?? null,
      runningMode: navigationState?.runningMode ?? 'FREE_RUN',
      route: finalRoute,
      distanceMeters,
      durationSeconds: finalDuration,
      averagePace: finalAveragePace,
      calories: Math.round(distanceMeters / 1_000 * 65),
      startedAt: startedAtRef.current.toISOString(),
      endedAt: new Date().toISOString(),
    })
    navigate('/running/complete', { replace: true, state: { record } })
  }

  if (!startPosition) return <Navigate to="/running/free" replace />

  return (
    <main className="live-running-page">
      {courseDetail ? (
        <CourseRouteMap
          routeCoordinates={courseDetail.routeCoordinates}
          waypoints={courseDetail.waypoints}
          currentPosition={position ? { lat: position.latitude, lng: position.longitude } : null}
          recordedPath={route.map(({ latitude, longitude }) => ({ lat: latitude, lng: longitude }))}
          className="running-map"
        />
      ) : (
        <FreeRunningMap currentPosition={position} recordedPath={route} />
      )}
      <section className="live-stat-panel">
        <div><strong>{formatDistance(distanceMeters)}</strong><span>km</span></div>
        <div><strong>{formatDuration(elapsedSeconds)}</strong><span>시간</span></div>
        <div><strong>{formatPace(averagePace)}</strong><span>평균 페이스</span></div>
      </section>
      <div className={`recording-status ${phase === 'paused' ? 'is-paused' : ''}`}><span />{phase === 'paused' ? '일시정지' : '기록 중'}</div>
      {locationError && <div className="live-location-error">{locationError}</div>}
      <div className="place-category-bar" aria-label="주변 장소 범례">
        <span><i className="place-camera"><RunningIcon name="camera" size={14} /></i>관광지</span>
        <span><i className="place-food">♨</i>맛집</span>
        <span><i className="place-cafe">▣</i>카페</span>
        <span><i className="place-store">▤</i>편의시설</span>
      </div>
      <section className="live-controls" aria-label="러닝 조작">
        <button className="round-control lock-control" type="button" aria-label="화면 잠금" onClick={() => setLocked(true)}><RunningIcon name="lock" /></button>
        <button className={`round-control pause-control ${phase === 'paused' ? 'is-paused' : ''}`} type="button" aria-label={phase === 'running' ? '일시정지' : '계속 달리기'} onClick={togglePause}><RunningIcon name={phase === 'running' ? 'pause' : 'play'} size={34} /></button>
        <button className="round-control stop-control" type="button" aria-label="러닝 종료" onClick={() => setShowEndSheet(true)}><RunningIcon name="stop" size={30} /></button>
        <button className="round-control camera-control" type="button" aria-label="사진 촬영" onClick={() => cameraInputRef.current?.click()}><RunningIcon name="camera" /></button>
      </section>
      <input ref={cameraInputRef} className="running-camera-input" type="file" accept="image/*" capture="environment" onChange={(event) => { if (event.target.files?.length) setPhotoCount((value) => value + 1); event.target.value = '' }} />
      {photoCount > 0 && <div className="photo-toast">사진 {photoCount}장이 러닝에 추가됐어요</div>}
      {locked && <ScreenLock onUnlock={() => setLocked(false)} />}
      {showEndSheet && (
        <div className="end-sheet-backdrop" onClick={() => !saving && setShowEndSheet(false)}>
          <section className="end-sheet" role="dialog" aria-modal="true" aria-labelledby="end-title" onClick={(event) => event.stopPropagation()}>
            <span className="sheet-handle" />
            <h2 id="end-title">러닝을 종료할까요?</h2>
            <p>지금까지 달린 코스와 기록이 저장돼요.</p>
            <div><button type="button" disabled={saving} onClick={() => setShowEndSheet(false)}>계속 달리기</button><button type="button" disabled={saving} onClick={finishRun}>{saving ? '저장 중…' : '종료 및 저장'}</button></div>
          </section>
        </div>
      )}
    </main>
  )
}

function readRunningNavigationState(value: unknown): RunningNavigationState | null {
  if (!value || typeof value !== 'object') return null
  const state = value as Record<string, unknown>
  const startPosition = isGeoPoint(state.startPosition) ? state.startPosition : undefined
  const courseId = typeof state.courseId === 'string' ? state.courseId : undefined
  const courseName = typeof state.courseName === 'string' ? state.courseName : undefined
  const runningMode = readRunningMode(state.runningMode, courseId)
  return { startPosition, courseId, courseName, runningMode }
}

function readRunningMode(value: unknown, courseId: string | undefined): RunningMode {
  if (value === 'COURSE_SELECT' || value === 'COURSE_CREATE' || value === 'FREE_RUN') {
    if (courseId || value === 'FREE_RUN') return value
  }
  return courseId ? 'COURSE_SELECT' : 'FREE_RUN'
}

function isGeoPoint(value: unknown): value is GeoPoint {
  if (!value || typeof value !== 'object') return false
  const point = value as Record<string, unknown>
  return typeof point.latitude === 'number' && typeof point.longitude === 'number'
}

function ScreenLock({ onUnlock }: { onUnlock: () => void }) {
  const startY = useRef<number | null>(null)
  const holdTimer = useRef<number | null>(null)
  const [drag, setDrag] = useState(0)

  const clearHold = () => {
    if (holdTimer.current !== null) window.clearTimeout(holdTimer.current)
    holdTimer.current = null
  }
  const pointerDown = (event: React.PointerEvent) => {
    startY.current = event.clientY
    event.currentTarget.setPointerCapture(event.pointerId)
    holdTimer.current = window.setTimeout(onUnlock, 1_200)
  }
  const pointerMove = (event: React.PointerEvent) => {
    if (startY.current === null) return
    const distance = Math.max(0, startY.current - event.clientY)
    setDrag(Math.min(distance, 90))
    if (distance > 72) { clearHold(); onUnlock() }
  }
  const pointerUp = () => { startY.current = null; setDrag(0); clearHold() }

  useEffect(() => clearHold, [])
  return (
    <div className="screen-lock" role="dialog" aria-modal="true" aria-label="화면 잠금 중">
      <div className="screen-lock-copy"><RunningIcon name="lock" size={34} /><strong>화면이 잠겼어요</strong><p>달리는 동안 잘못 눌리지 않아요</p></div>
      <button type="button" onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={pointerUp} style={{ transform: `translateY(-${drag}px)` }}><RunningIcon name="lock" /><span>위로 밀거나 길게 눌러 해제</span><b>⌃</b></button>
    </div>
  )
}
