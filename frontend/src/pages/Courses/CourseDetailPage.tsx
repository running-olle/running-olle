import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { CourseRouteMap } from '../../features/course/CourseRouteMap'
import { courseService } from '../../features/course/courseService'
import type { CourseDetail, CourseDifficulty, CourseType } from '../../features/course/types'
import { Badge, Button, ErrorState, Icon, Modal, Spinner } from '../../components/ui'

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
        <ErrorState
          className="course-detail-empty"
          icon={<Icon name="course" />}
          title="코스를 불러오지 못했어요"
          description="삭제되었거나 볼 수 없는 코스일 수 있어요."
          action={<Link to="/courses">코스 탐색으로</Link>}
        />
      </section>
    )
  }

  if (!course) {
    return (
      <section className="course-detail-page">
        <div className="course-library-loading"><Spinner label="코스를 불러오는 중" /><span>코스를 불러오는 중이에요</span></div>
      </section>
    )
  }

  const hasSurface = course.surfaceAsphaltPct > 0 || course.surfaceDirtPct > 0 || course.surfaceStairsPct > 0
  const showBookmarkAction = !course.createdByMe
  const creatorName = course.createdByMe ? '나' : course.creatorNickname || '러닝올레 러너'

  return (
    <section className="course-detail-page">
      <div className="course-detail-title">
        <Badge variant={course.courseType === 'SPOT_COURSE' ? 'spot' : 'success'}>{courseTypeLabel[course.courseType]}</Badge>
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
        {course.createdByMe && <Badge variant="brand">내가 만든 코스</Badge>}
        {course.bookmarkedByMe && <Badge variant="warning">저장됨</Badge>}
        {!course.isPublic && <Badge variant="neutral">비공개</Badge>}
      </div>

      <section className="course-detail-creator">
        <span>{creatorName.slice(0, 1)}</span>
        <div>
          <small>작성자</small>
          <strong>{creatorName}</strong>
          <p>{formatCreatedAt(course.createdAt)} 등록 · {course.isPublic ? '공개 코스' : '비공개 코스'}</p>
        </div>
        <Button variant="tertiary" size="sm" onClick={() => setIsInfoOpen(true)}>소개 보기</Button>
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

      <Modal
        open={isInfoOpen}
        title="코스 소개"
        description={course.description || '작성자가 아직 코스 소개를 남기지 않았어요.'}
        onClose={() => setIsInfoOpen(false)}
        className="course-detail-info-modal"
      >
        <Badge variant={course.courseType === 'SPOT_COURSE' ? 'spot' : 'success'}>{courseTypeLabel[course.courseType]}</Badge>
        <dl>
          <div><dt>작성자</dt><dd>{creatorName}</dd></div>
          <div><dt>저장 상태</dt><dd>{course.bookmarkedByMe ? '저장됨' : course.createdByMe ? '내 코스' : '미저장'}</dd></div>
          <div><dt>완주 수</dt><dd>{course.completionCount}회</dd></div>
          <div><dt>평점</dt><dd>{course.ratingAvg.toFixed(1)}</dd></div>
        </dl>
      </Modal>

      <div className="course-detail-footer" data-has-bookmark={showBookmarkAction}>
        {showBookmarkAction && (
          <Button
            variant="secondary"
            size="lg"
            className="course-detail-bookmark"
            disabled={isSavingBookmark}
            onClick={toggleBookmark}
          >
            <span>
              <Icon name="bookmark" fill={course.bookmarkedByMe || isSavingBookmark ? 'currentColor' : 'none'} />
              {isSavingBookmark ? '처리 중' : course.bookmarkedByMe ? '저장 취소' : '저장하기'}
            </span>
          </Button>
        )}
        <Button variant="primary" size="lg" className="course-detail-start" onClick={startCourseRun}>이 코스로 달리기</Button>
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
