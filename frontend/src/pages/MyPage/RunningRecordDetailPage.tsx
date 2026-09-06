import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { CourseRouteMap } from '../../features/course/CourseRouteMap'
import { myPageService } from '../../features/mypage/myPageService'
import type { RunRecordDetail, RunRouteCoordinate } from '../../features/mypage/types'
import './mypage.css'

const courseTypeLabels = {
  RUNNING_COURSE: '러닝 코스',
  SPOT_COURSE: '스팟 코스',
} as const

const difficultyLabels = {
  LOW: '쉬움',
  MID: '보통',
  HIGH: '어려움',
} as const

const runningModeLabels = {
  COURSE_SELECT: '코스 선택 달리기',
  COURSE_CREATE: '코스 만들고 달리기',
  FREE_RUN: '즉시 달리기',
} as const

export function RunningRecordDetailPage() {
  const navigate = useNavigate()
  const { recordId } = useParams()
  const [record, setRecord] = useState<RunRecordDetail | null>(null)
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    if (!recordId) return
    let ignore = false
    setRecord(null)
    setHasError(false)

    myPageService.run(recordId)
      .then((data) => {
        if (!ignore) setRecord(data)
      })
      .catch(() => {
        if (!ignore) {
          setRecord(null)
          setHasError(true)
        }
      })

    return () => {
      ignore = true
    }
  }, [recordId])

  if (hasError) {
    return (
      <div className="my-screen">
        <PageHeader title="러닝 상세" onBack={() => navigate(-1)} />
        <main className="my-content">
          <div className="my-empty">
            <span><BackIcon /></span>
            <strong>러닝 기록을 불러오지 못했어요</strong>
            <p>삭제되었거나 내 기록이 아닐 수 있어요.</p>
            <Link className="primary-link" to="/mypage/history">히스토리로 돌아가기</Link>
          </div>
        </main>
      </div>
    )
  }

  if (!record) {
    return (
      <div className="my-screen">
        <PageHeader title="러닝 상세" onBack={() => navigate(-1)} />
        <main className="my-content">
          <div className="my-loading"><div className="spinner" /><span>러닝 기록을 불러오는 중...</span></div>
        </main>
      </div>
    )
  }

  const hasCourse = record.courseId !== null
  const courseTypeLabel = record.courseType ? courseTypeLabels[record.courseType] : '자유 러닝'
  const difficultyLabel = record.courseDifficulty ? difficultyLabels[record.courseDifficulty] : null
  const hasMeaningfulRecordedRoute = isMeaningfulRoute(record.recordedRouteCoordinates, record.distanceKm)
  const recordedPathForMap = hasMeaningfulRecordedRoute || !hasCourse ? record.recordedRouteCoordinates : []
  const mapFitTarget = hasMeaningfulRecordedRoute ? 'recorded' : hasCourse ? 'planned' : 'recorded'
  const hasCourseDeviation = hasCourse
    && hasMeaningfulRecordedRoute
    && isRouteDeviatedFromCourse(record.recordedRouteCoordinates, record.plannedRouteCoordinates)

  return (
    <div className="my-screen run-detail-screen">
      <PageHeader title="러닝 상세" onBack={() => navigate(-1)} />
      <main className="my-content run-detail-content">
        <section className="run-detail-hero">
          <span>{runningModeLabels[record.runningMode]}</span>
          <h2>{record.courseName ?? '나만의 자유 러닝'}</h2>
          <p>{formatDateTime(record.startedAt)}</p>
        </section>

        <div className="run-detail-map-card">
          <CourseRouteMap
            routeCoordinates={record.plannedRouteCoordinates}
            recordedPath={recordedPathForMap}
            waypoints={record.courseWaypoints}
            fitTarget={mapFitTarget}
            plannedRouteStyle={{ strokeWeight: 4, strokeColor: '#2563EB', strokeOpacity: 0.42, strokeStyle: 'shortdash' }}
            recordedRouteStyle={{ strokeWeight: 7, strokeColor: '#FF6F0F', strokeOpacity: 0.96, strokeStyle: 'solid' }}
            showCurrentPositionMarker={false}
            className="run-detail-map"
            showZoomControls
          />
        </div>
        <div className="run-detail-legend">
          {hasCourse && <span><i className="planned" />원본 코스</span>}
          {(hasMeaningfulRecordedRoute || !hasCourse) && <span><i className="recorded" />실제 기록</span>}
        </div>
        {hasCourse && !hasMeaningfulRecordedRoute && (
          <p className="run-detail-map-notice">실제 위치 기록이 충분하지 않아 원본 코스 기준으로 보여줘요.</p>
        )}
        {hasCourseDeviation && (
          <p className="run-detail-map-notice">원본 코스를 벗어난 구간이 있어 실제로 뛴 경로 기준으로 보여줘요.</p>
        )}

        <section className="run-detail-stats" aria-label="러닝 기록 통계">
          <div><span>달린 거리</span><strong>{record.distanceKm.toFixed(1)}<small>km</small></strong></div>
          <div><span>러닝 시간</span><strong>{formatDuration(record.durationSeconds)}</strong></div>
          <div><span>평균 페이스</span><strong>{formatPace(record.averagePace)}<small>/km</small></strong></div>
          <div><span>소모 칼로리</span><strong>{formatCalories(record.calories)}<small>kcal</small></strong></div>
        </section>

        <section className="run-detail-card">
          <h2>기록 정보</h2>
          <dl className="run-detail-list">
            <div><dt>출발</dt><dd>{formatDateTime(record.startedAt)}</dd></div>
            <div><dt>완주</dt><dd>{formatDateTime(record.endedAt)}</dd></div>
            <div><dt>진입 방식</dt><dd>{runningModeLabels[record.runningMode]}</dd></div>
            <div><dt>누적 고도</dt><dd>{record.elevationGainM?.toFixed(0) ?? '0'}m</dd></div>
          </dl>
        </section>

        <section className="run-detail-card">
          <h2>연결된 코스</h2>
          {hasCourse ? (
            <>
              <div className="run-detail-course-summary">
                <span>{courseTypeLabel}</span>
                {difficultyLabel && <span>{difficultyLabel}</span>}
              </div>
              <strong>{record.courseName}</strong>
              {record.courseDescription && <p>{record.courseDescription}</p>}
              <Link className="outline-link" to={`/courses/${record.courseId}`}>코스 상세보기</Link>
            </>
          ) : (
            <p className="run-detail-muted">코스 없이 즉시 달리기로 저장한 기록이에요.</p>
          )}
        </section>

        {record.courseWaypoints.length > 0 && (
          <section className="course-detail-waypoints run-detail-waypoints">
            <h2>코스 경유지</h2>
            <ol>
              {record.courseWaypoints.map((waypoint, index) => (
                <li key={waypoint.id ?? `${waypoint.name}-${index}`}>
                  <span>{index + 1}</span>
                  <div>
                    <strong>{waypoint.name}</strong>
                    {waypoint.description && <p>{waypoint.description}</p>}
                    <small>출발점부터 {waypoint.distanceFromStartKm?.toFixed(1) ?? '0.0'}km</small>
                  </div>
                </li>
              ))}
            </ol>
          </section>
        )}
      </main>
    </div>
  )
}

function PageHeader({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <header className="my-header">
      <button type="button" onClick={onBack} aria-label="뒤로"><BackIcon /></button>
      <h1>{title}</h1>
      <div />
    </header>
  )
}

function BackIcon() {
  return (
    <svg aria-hidden width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="m15 18-6-6 6-6" />
    </svg>
  )
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatDuration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}시간 ${minutes}분`
  return `${minutes}분`
}

function formatPace(value: number | null) {
  if (!value) return '-'
  const minutes = Math.floor(value)
  const seconds = Math.round((value - minutes) * 60)
  return `${minutes}'${String(seconds).padStart(2, '0')}"`
}

function formatCalories(value: number | null) {
  if (value === null) return '0'
  return value.toFixed(0)
}

function isMeaningfulRoute(coordinates: RunRouteCoordinate[], distanceKm: number) {
  return coordinates.length >= 2 && (distanceKm >= 0.05 || calculateRouteDistanceKm(coordinates) >= 0.05)
}

function calculateRouteDistanceKm(coordinates: RunRouteCoordinate[]) {
  let total = 0
  for (let index = 1; index < coordinates.length; index += 1) {
    total += distanceKm(coordinates[index - 1], coordinates[index])
  }
  return total
}

function isRouteDeviatedFromCourse(recordedRoute: RunRouteCoordinate[], plannedRoute: RunRouteCoordinate[]) {
  if (recordedRoute.length < 2 || plannedRoute.length < 2) return false
  const samples = sampleCoordinates(recordedRoute, 24)
  const deviatedSamples = samples.filter((coordinate) => nearestDistanceKm(coordinate, plannedRoute) > 0.3)
  return deviatedSamples.length / samples.length >= 0.35
}

function sampleCoordinates(coordinates: RunRouteCoordinate[], maxSamples: number) {
  if (coordinates.length <= maxSamples) return coordinates
  const lastIndex = coordinates.length - 1
  return Array.from({ length: maxSamples }, (_, index) => {
    const sourceIndex = Math.round(index * lastIndex / (maxSamples - 1))
    return coordinates[sourceIndex]
  })
}

function nearestDistanceKm(target: RunRouteCoordinate, coordinates: RunRouteCoordinate[]) {
  return coordinates.reduce((nearest, coordinate) => Math.min(nearest, distanceKm(target, coordinate)), Number.POSITIVE_INFINITY)
}

function distanceKm(a: RunRouteCoordinate, b: RunRouteCoordinate) {
  const earthRadiusKm = 6371
  const dLat = toRad(b.lat - a.lat)
  const dLng = toRad(b.lng - a.lng)
  const lat1 = toRad(a.lat)
  const lat2 = toRad(b.lat)
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * earthRadiusKm * Math.asin(Math.sqrt(h))
}

function toRad(value: number) {
  return value * Math.PI / 180
}
