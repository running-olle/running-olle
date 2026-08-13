import { useEffect, useMemo, useRef, useState } from 'react'
import RunningMap from './components/RunningMap'
import { DEMO_COURSES, GANGNAM_HOLLYS_COURSE } from './data/demoCourse'
import type { GeoPoint, RunPhase } from './models/running'
import { calculateRouteProgress, distanceBetween, formatDistance, formatDuration, formatPace } from './utils/geo'

const GPS_OPTIONS: PositionOptions = {
  enableHighAccuracy: true,
  maximumAge: 1_000,
  timeout: 15_000,
}

function positionToPoint(position: GeolocationPosition): GeoPoint {
  return {
    latitude: position.coords.latitude,
    longitude: position.coords.longitude,
    accuracy: position.coords.accuracy,
    timestamp: position.timestamp,
  }
}

function getLocationErrorMessage(error: GeolocationPositionError) {
  if (error.code === error.PERMISSION_DENIED) {
    return '위치 권한이 거부되었습니다. 브라우저 설정에서 위치 권한을 허용해 주세요.'
  }
  if (error.code === error.POSITION_UNAVAILABLE) {
    return '현재 위치를 확인할 수 없습니다. GPS와 위치 서비스를 켜 주세요.'
  }
  return '위치 확인 시간이 초과되었습니다. 하늘이 잘 보이는 곳에서 다시 시도해 주세요.'
}

function App() {
  const [selectedCourse, setSelectedCourse] = useState(GANGNAM_HOLLYS_COURSE)
  const [phase, setPhase] = useState<RunPhase>('ready')
  const [currentPosition, setCurrentPosition] = useState<GeoPoint | null>(null)
  const [recordedPath, setRecordedPath] = useState<GeoPoint[]>([])
  const [distanceMeters, setDistanceMeters] = useState(0)
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [locationError, setLocationError] = useState<string | null>(null)
  const [followPosition, setFollowPosition] = useState(false)
  const lastRecordedPosition = useRef<GeoPoint | null>(null)
  const hasKakaoMapKey = Boolean(import.meta.env.VITE_KAKAO_MAP_APP_KEY)

  useEffect(() => {
    if (phase !== 'running') return
    const timer = window.setInterval(() => setElapsedSeconds((seconds) => seconds + 1), 1_000)
    return () => window.clearInterval(timer)
  }, [phase])

  useEffect(() => {
    if (phase !== 'running') return
    if (!('geolocation' in navigator)) {
      setLocationError('이 브라우저에서는 위치 추적을 지원하지 않습니다.')
      return
    }

    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        const nextPoint = positionToPoint(position)
        setCurrentPosition(nextPoint)
        setLocationError(null)

        if (nextPoint.accuracy && nextPoint.accuracy > 80) return

        const previousPoint = lastRecordedPosition.current
        if (!previousPoint) {
          lastRecordedPosition.current = nextPoint
          setRecordedPath((path) => [...path, nextPoint])
          return
        }

        const segmentDistance = distanceBetween(previousPoint, nextPoint)
        if (segmentDistance < 3 || segmentDistance > 200) return

        lastRecordedPosition.current = nextPoint
        setDistanceMeters((distance) => distance + segmentDistance)
        setRecordedPath((path) => [...path, nextPoint])
      },
      (error) => setLocationError(getLocationErrorMessage(error)),
      GPS_OPTIONS,
    )

    return () => navigator.geolocation.clearWatch(watchId)
  }, [phase])

  const routeProgress = useMemo(
    () => calculateRouteProgress(selectedCourse.path, currentPosition),
    [currentPosition, selectedCourse],
  )
  const averagePace = distanceMeters > 10 ? elapsedSeconds / 60 / (distanceMeters / 1_000) : null

  const checkCurrentLocation = () => {
    if (!('geolocation' in navigator)) {
      setLocationError('이 브라우저에서는 위치 확인을 지원하지 않습니다.')
      return
    }
    setLocationError('현재 위치를 확인하고 있습니다...')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCurrentPosition(positionToPoint(position))
        setLocationError(null)
        setFollowPosition(true)
      },
      (error) => setLocationError(getLocationErrorMessage(error)),
      GPS_OPTIONS,
    )
  }

  const startRun = () => {
    setRecordedPath([])
    setDistanceMeters(0)
    setElapsedSeconds(0)
    setLocationError(null)
    setFollowPosition(true)
    lastRecordedPosition.current = null
    setPhase('running')
  }

  const resetRun = () => {
    lastRecordedPosition.current = null
    setRecordedPath([])
    setDistanceMeters(0)
    setElapsedSeconds(0)
    setLocationError(null)
    setFollowPosition(false)
    setPhase('ready')
  }

  if (phase === 'summary') {
    return (
      <main className="app-shell">
        <section className="summary-screen">
          <div className="summary-check" aria-hidden="true">✓</div>
          <p className="eyebrow">러닝 완료</p>
          <h1>{selectedCourse.name} 완료</h1>
          <p className="summary-message">수고했어요! 기록은 현재 기기에 저장하지 않는 데모입니다.</p>
          <div className="summary-stats">
            <Stat label="거리" value={formatDistance(distanceMeters)} />
            <Stat label="시간" value={formatDuration(elapsedSeconds)} />
            <Stat label="평균 페이스" value={formatPace(averagePace)} />
          </div>
          <div className="summary-map">
            <RunningMap key={selectedCourse.id} course={selectedCourse} currentPosition={currentPosition} recordedPath={recordedPath} progress={routeProgress} followPosition={false} />
          </div>
          <div className="summary-detail">
            <span>코스 진행률</span>
            <strong>{Math.round(routeProgress.percent)}%</strong>
          </div>
          <button className="primary-button" type="button" onClick={resetRun}>다시 테스트하기</button>
        </section>
      </main>
    )
  }

  const pauseRun = () => {
    lastRecordedPosition.current = null
    setPhase('paused')
  }
  const resumeRun = () => {
    lastRecordedPosition.current = null
    setPhase('running')
  }

  return (
    <main className="app-shell">
      <section className="run-screen">
        <header className="app-header">
          <div>
            <p className="brand">RUNNING OLLE</p>
            <h1>{selectedCourse.name}</h1>
          </div>
          <span className={`status-pill status-${phase}`}>
            {phase === 'ready' ? '준비' : phase === 'paused' ? '일시정지' : '기록 중'}
          </span>
        </header>

        {phase === 'ready' && (
          <section className="course-selector" aria-label="러닝 코스 선택">
            <p>코스 선택</p>
            <div className="course-options">
              {DEMO_COURSES.map((course) => (
                <button
                  key={course.id}
                  className={`course-option ${selectedCourse.id === course.id ? 'course-option-selected' : ''}`}
                  type="button"
                  aria-pressed={selectedCourse.id === course.id}
                  onClick={() => {
                    setSelectedCourse(course)
                    setFollowPosition(false)
                  }}
                >
                  <strong>{course.name}</strong>
                  <span>{course.distanceLabel} · {course.estimatedTime}</span>
                </button>
              ))}
            </div>
          </section>
        )}

        <div className="course-meta" aria-label="코스 정보">
          <span>{selectedCourse.distanceLabel}</span>
          <span>{selectedCourse.estimatedTime}</span>
          <span>{selectedCourse.difficulty}</span>
        </div>

        <section className="map-card">
          <RunningMap key={selectedCourse.id} course={selectedCourse} currentPosition={currentPosition} recordedPath={recordedPath} progress={routeProgress} followPosition={followPosition} />
          <button
            className="location-button"
            type="button"
            aria-label="현재 위치로 지도 이동"
            onClick={() => currentPosition ? setFollowPosition((value) => !value) : checkCurrentLocation()}
          >◎</button>
          <div className="map-legend">
            <span><i className="legend-course" /> 예정 코스</span>
            <span><i className="legend-run" /> 이동 경로</span>
          </div>
        </section>

        {!hasKakaoMapKey && (
          <div className="notice notice-warning">
            <strong>카카오맵 키가 필요합니다.</strong>
            <span><code>.env</code>에 <code>VITE_KAKAO_MAP_APP_KEY</code>를 설정해 주세요.</span>
          </div>
        )}
        {locationError && <div className="notice">{locationError}</div>}

        <section className="live-panel">
          <div className="primary-metric">
            <span>경과 시간</span>
            <strong>{formatDuration(elapsedSeconds)}</strong>
          </div>
          <div className="metric-row">
            <Stat label="이동 거리" value={formatDistance(distanceMeters)} />
            <Stat label="평균 페이스" value={formatPace(averagePace)} />
            <Stat label="코스 이탈 거리" value={currentPosition ? formatDistance(routeProgress.distanceFromRouteMeters) : '--'} />
          </div>
          <div className="progress-block">
            <div className="progress-label"><span>코스 진행률</span><strong>{Math.round(routeProgress.percent)}%</strong></div>
            <div className="progress-track"><span style={{ width: `${routeProgress.percent}%` }} /></div>
          </div>
        </section>

        <div className="run-actions">
          {phase === 'ready' && (
            <>
              <button className="secondary-button" type="button" onClick={checkCurrentLocation}>내 위치 확인</button>
              <button className="primary-button" type="button" onClick={startRun} disabled={!hasKakaoMapKey}>러닝 시작</button>
            </>
          )}
          {phase === 'running' && (
            <>
              <button className="secondary-button" type="button" onClick={pauseRun}>일시정지</button>
              <button className="danger-button" type="button" onClick={() => setPhase('summary')}>러닝 종료</button>
            </>
          )}
          {phase === 'paused' && (
            <>
              <button className="primary-button" type="button" onClick={resumeRun}>계속 달리기</button>
              <button className="danger-button" type="button" onClick={() => setPhase('summary')}>러닝 종료</button>
            </>
          )}
        </div>
        <p className="privacy-note">GPS 좌표는 이 화면의 기록 계산에만 사용되며 서버로 전송되지 않습니다.</p>
      </section>
    </main>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return <div className="stat"><span>{label}</span><strong>{value}</strong></div>
}

export default App
