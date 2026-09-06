import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { CourseRouteMap } from '../../features/course/CourseRouteMap'
import { courseService } from '../../features/course/courseService'
import type { CourseDetail } from '../../features/course/types'
import { FreeRunningMap } from '../../features/running/FreeRunningMap'
import { RunningIcon } from '../../features/running/RunningIcon'
import { getLocationErrorMessage, GPS_OPTIONS, positionToPoint } from '../../features/running/runningUtils'
import type { GeoPoint, RunningMode } from '../../features/running/types'

type CourseRunState = {
  courseId: string
  courseName: string
  runningMode?: RunningMode
}

export function FreeRunReadyPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const selectedCourse = useMemo(() => readCourseRunState(location.state as unknown), [location.state])
  const [position, setPosition] = useState<GeoPoint | null>(null)
  const [courseDetail, setCourseDetail] = useState<CourseDetail | null>(null)
  const [courseLoadFailed, setCourseLoadFailed] = useState(false)
  const [locationMessage, setLocationMessage] = useState('현재 위치를 찾고 있어요…')
  const [countdown, setCountdown] = useState<number | null>(null)

  const findLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setLocationMessage('이 기기에서는 위치 기능을 사용할 수 없어요.')
      return
    }
    setLocationMessage('현재 위치를 찾고 있어요…')
    navigator.geolocation.getCurrentPosition((result) => {
      setPosition(positionToPoint(result))
      setLocationMessage('현재 위치를 확인했어요')
    }, (error) => setLocationMessage(getLocationErrorMessage(error)), GPS_OPTIONS)
  }, [])

  useEffect(() => { findLocation() }, [findLocation])

  useEffect(() => {
    if (!selectedCourse) {
      setCourseDetail(null)
      setCourseLoadFailed(false)
      return
    }
    let ignore = false
    setCourseDetail(null)
    setCourseLoadFailed(false)
    courseService.getCourse(selectedCourse.courseId)
      .then((detail) => {
        if (!ignore) setCourseDetail(detail)
      })
      .catch(() => {
        if (!ignore) setCourseLoadFailed(true)
      })
    return () => {
      ignore = true
    }
  }, [selectedCourse])

  useEffect(() => {
    if (countdown === null) return
    if (countdown === 0) {
      navigate('/running/live', {
        replace: true,
        state: {
          startPosition: position,
          courseId: selectedCourse?.courseId,
          courseName: selectedCourse?.courseName,
          runningMode: selectedCourse?.runningMode ?? (selectedCourse ? 'COURSE_SELECT' : 'FREE_RUN'),
        },
      })
      return
    }
    const timer = window.setTimeout(() => setCountdown((value) => value === null ? null : value - 1), 1_000)
    return () => window.clearTimeout(timer)
  }, [countdown, navigate, position, selectedCourse])

  return (
    <main className="free-ready-page">
      <header className="running-flow-header">
        <button type="button" aria-label="뒤로 가기" onClick={() => navigate('/running')}><RunningIcon name="back" /></button>
        <strong>{selectedCourse ? '코스 달리기' : '즉시 달리기'}</strong>
        <span />
      </header>
      <section className="free-ready-content">
        {courseDetail ? (
          <CourseRouteMap
            routeCoordinates={courseDetail.routeCoordinates}
            waypoints={courseDetail.waypoints}
            currentPosition={position ? { lat: position.latitude, lng: position.longitude } : null}
            className="running-map"
            showZoomControls
          />
        ) : selectedCourse && !courseLoadFailed ? (
          <div className="running-map course-route-loading">
            <div className="spinner" />
            <span>저장된 코스 경로를 불러오는 중이에요</span>
          </div>
        ) : (
          <FreeRunningMap currentPosition={position} followPosition={!selectedCourse} />
        )}
        <button className="map-location-button" type="button" aria-label="현재 위치 다시 찾기" onClick={findLocation}><RunningIcon name="location" /></button>
        <div className="ready-location-card">
          <span className={position ? 'is-ready' : ''} />
          <div>
            <strong>{selectedCourse ? selectedCourse.courseName : position ? '달릴 준비가 됐어요' : '위치를 확인하고 있어요'}</strong>
            <p>{courseLoadFailed ? '코스 경로를 불러오지 못했어요. 현재 위치 지도로 시작합니다.' : locationMessage}</p>
          </div>
        </div>
      </section>
      <footer className="free-ready-footer">
        <p>{selectedCourse ? `${selectedCourse.courseName} 코스를 시작해요` : '안전한 장소에서 시작해 주세요'}</p>
        <button type="button" disabled={!position || countdown !== null} onClick={() => setCountdown(3)}>러닝 시작</button>
      </footer>
      {countdown !== null && countdown > 0 && <div className="running-countdown" role="status"><span key={countdown}>{countdown}</span><p>준비하세요!</p></div>}
    </main>
  )
}

function readCourseRunState(value: unknown): CourseRunState | null {
  if (!value || typeof value !== 'object') return null
  const state = value as Record<string, unknown>
  if (typeof state.courseId !== 'string' || typeof state.courseName !== 'string') return null
  const runningMode = state.runningMode === 'COURSE_CREATE' ? 'COURSE_CREATE' : 'COURSE_SELECT'
  return {
    courseId: state.courseId,
    courseName: state.courseName,
    runningMode,
  }
}
