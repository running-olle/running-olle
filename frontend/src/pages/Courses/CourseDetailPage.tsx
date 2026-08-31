import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { CourseRouteMap } from '../../features/course/CourseRouteMap'
import { courseService } from '../../features/course/courseService'
import type { CourseDetail, CourseDifficulty, CourseType } from '../../features/course/types'

const courseTypeLabel: Record<CourseType, string> = {
  RUNNING_COURSE: '러닝 코스',
  SPOT_COURSE: '스팟 코스',
}

const difficultyLabel: Record<CourseDifficulty, string> = {
  LOW: '쉬움',
  MID: '보통',
  HIGH: '어려움',
}

export function CourseDetailPage() {
  const navigate = useNavigate()
  const { courseId } = useParams()
  const [course, setCourse] = useState<CourseDetail | null>(null)
  const [hasError, setHasError] = useState(false)
  const [bookmarkError, setBookmarkError] = useState(false)
  const [isSavingBookmark, setIsSavingBookmark] = useState(false)
  const [isInfoOpen, setIsInfoOpen] = useState(false)

  useEffect(() => {
    if (!courseId) return
    let ignore = false
    setCourse(null)
    setHasError(false)
    setBookmarkError(false)

    courseService.getCourse(courseId)
      .then((data) => {
        if (!ignore) setCourse(data)
      })
      .catch(() => {
        if (!ignore) {
          setCourse(null)
          setHasError(true)
        }
      })

    return () => {
      ignore = true
    }
  }, [courseId])

  const startCourseRun = () => {
    if (!course) return
    navigate('/running/free', {
      state: {
        courseId: course.id,
        courseName: course.name,
        runningMode: 'COURSE_SELECT',
      },
    })
  }

  const toggleBookmark = async () => {
    if (!course || course.createdByMe || isSavingBookmark) return
    setIsSavingBookmark(true)
    setBookmarkError(false)
    try {
      if (course.bookmarkedByMe) {
        await courseService.unbookmarkCourse(course.id)
        setCourse((current) => current
          ? { ...current, bookmarkedByMe: false, bookmarkId: null }
          : current)
      } else {
        const response = await courseService.bookmarkCourse(course.id)
        setCourse((current) => current
          ? { ...current, bookmarkedByMe: true, bookmarkId: response.bookmarkId }
          : current)
      }
    } catch {
      setBookmarkError(true)
    } finally {
      setIsSavingBookmark(false)
    }
  }

  if (hasError) {
    return (
      <section className="course-detail-page">
        <div className="course-detail-empty">
          <strong>코스를 불러오지 못했어요</strong>
          <p>삭제되었거나 볼 수 없는 코스일 수 있어요.</p>
          <Link to="/courses">코스 탐색으로</Link>
        </div>
      </section>
    )
  }

  if (!course) {
    return (
      <section className="course-detail-page">
        <div className="course-library-loading"><div className="spinner" /><span>코스를 불러오는 중이에요</span></div>
      </section>
    )
  }

  const hasSurface = course.surfaceAsphaltPct > 0 || course.surfaceDirtPct > 0 || course.surfaceStairsPct > 0
  const showBookmarkAction = !course.createdByMe
  const creatorName = course.createdByMe ? '나' : course.creatorNickname || '러닝올레 러너'

  return (
    <section className="course-detail-page">
      <div className="course-detail-title">
        <span>{courseTypeLabel[course.courseType]}</span>
        <h1>{course.name}</h1>
        {course.description && <p>{course.description}</p>}
      </div>

      <div className="course-detail-map-card">
        <CourseRouteMap
          routeCoordinates={course.routeCoordinates}
          waypoints={course.waypoints}
          className="course-detail-map"
          showZoomControls
        />
      </div>

      <div className="course-detail-badges">
        {course.createdByMe && <span>내가 만든 코스</span>}
        {course.bookmarkedByMe && <span>저장됨</span>}
        {!course.isPublic && <span>비공개</span>}
      </div>

      <section className="course-detail-creator">
        <span>{creatorName.slice(0, 1)}</span>
        <div>
          <small>작성자</small>
          <strong>{creatorName}</strong>
          <p>{formatCreatedAt(course.createdAt)} 등록 · {course.isPublic ? '공개 코스' : '비공개 코스'}</p>
        </div>
        <button type="button" onClick={() => setIsInfoOpen(true)}>소개 보기</button>
      </section>

      <section className="course-detail-stats" aria-label="코스 통계">
        <div><span>총 거리</span><strong>{course.distanceKm.toFixed(1)}<small>km</small></strong></div>
        <div><span>예상 시간</span><strong>{course.estimatedDurationMinutes}<small>분</small></strong></div>
        <div><span>누적 고도</span><strong>{course.elevationGainM?.toFixed(0) ?? 0}<small>m</small></strong></div>
        <div><span>난이도</span><strong>{difficultyLabel[course.difficulty]}</strong></div>
      </section>

      {hasSurface && (
        <div className="course-detail-surface">
          <span>포장 {course.surfaceAsphaltPct.toFixed(0)}%</span>
          <span>흙길 {course.surfaceDirtPct.toFixed(0)}%</span>
          <span>계단 {course.surfaceStairsPct.toFixed(0)}%</span>
        </div>
      )}

      <section className="course-detail-waypoints">
        <h2>경유지</h2>
        <ol>
          {course.waypoints.map((waypoint, index) => (
            <li key={waypoint.id ?? `${waypoint.name}-${index}`}>
              <span>{index + 1}</span>
              <div>
                <strong>{waypoint.name}</strong>
                {waypoint.description && <p>{waypoint.description}</p>}
                <small>
                  출발점부터 {waypoint.distanceFromStartKm?.toFixed(1) ?? '0.0'}km
                  {waypoint.tourContentId ? ' · TourAPI 정보 저장됨' : ''}
                </small>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {bookmarkError && (
        <p className="course-detail-action-error">코스 저장 상태를 바꾸지 못했어요. 잠시 후 다시 시도해 주세요.</p>
      )}

      {isInfoOpen && (
        <div className="course-detail-info-backdrop" role="dialog" aria-modal="true" aria-labelledby="course-detail-info-title" onClick={() => setIsInfoOpen(false)}>
          <div className="course-detail-info-modal" onClick={(event) => event.stopPropagation()}>
            <button type="button" aria-label="닫기" onClick={() => setIsInfoOpen(false)}>×</button>
            <span>{courseTypeLabel[course.courseType]}</span>
            <h2 id="course-detail-info-title">코스 소개</h2>
            <p>{course.description || '작성자가 아직 코스 소개를 남기지 않았어요.'}</p>
            <dl>
              <div><dt>작성자</dt><dd>{creatorName}</dd></div>
              <div><dt>저장 상태</dt><dd>{course.bookmarkedByMe ? '저장됨' : course.createdByMe ? '내 코스' : '미저장'}</dd></div>
              <div><dt>완주 수</dt><dd>{course.completionCount}회</dd></div>
              <div><dt>평점</dt><dd>{course.ratingAvg.toFixed(1)}</dd></div>
            </dl>
          </div>
        </div>
      )}

      <div className="course-detail-footer" data-has-bookmark={showBookmarkAction}>
        {showBookmarkAction && (
          <button
            className="course-detail-bookmark"
            type="button"
            disabled={isSavingBookmark}
            onClick={toggleBookmark}
          >
            <span>
              <DetailBookmarkIcon filled={course.bookmarkedByMe || isSavingBookmark} />
              {isSavingBookmark ? '처리 중' : course.bookmarkedByMe ? '저장 취소' : '저장하기'}
            </span>
          </button>
        )}
        <button className="course-detail-start" type="button" onClick={startCourseRun}>이 코스로 달리기</button>
      </div>
    </section>
  )
}

function formatCreatedAt(value: string) {
  return new Date(value).toLocaleDateString('ko-KR', {
    month: 'long',
    day: 'numeric',
  })
}

function DetailBookmarkIcon({ filled }: { filled: boolean }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M7 4.75A2.25 2.25 0 0 1 9.25 2.5h5.5A2.25 2.25 0 0 1 17 4.75v15.1a.65.65 0 0 1-1.02.53L12 17.6l-3.98 2.78A.65.65 0 0 1 7 19.85V4.75Z"
        fill={filled ? 'currentColor' : 'none'}
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  )
}
