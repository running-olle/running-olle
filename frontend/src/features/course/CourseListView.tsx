import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CourseRouteMap } from './CourseRouteMap'
import { courseService } from './courseService'
import type { CourseDifficulty, CourseListFilter, CourseListItem, CourseListScope, CourseType } from './types'

const FILTER_OPTIONS: { value: CourseListFilter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'RUNNING_COURSE', label: '러닝코스' },
  { value: 'SPOT_COURSE', label: '스팟코스' },
  { value: 'CREATED', label: '내가만든코스' },
]

const courseTypeLabel: Record<CourseType, string> = {
  RUNNING_COURSE: '러닝 코스',
  SPOT_COURSE: '스팟 코스',
}

const difficultyLabel: Record<CourseDifficulty, string> = {
  LOW: '쉬움',
  MID: '보통',
  HIGH: '어려움',
}

type CourseListViewProps = {
  scope: CourseListScope
  title: string
  subtitle: string
  emptyTitle: string
  emptyDescription: string
  onRemoveBookmark?: (bookmarkId: string) => Promise<void>
  showHeader?: boolean
  showCreateAction?: boolean
}

export function CourseListView({
  scope,
  title,
  subtitle,
  emptyTitle,
  emptyDescription,
  onRemoveBookmark,
  showHeader = true,
  showCreateAction = true,
}: CourseListViewProps) {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<CourseListFilter>('ALL')
  const [courses, setCourses] = useState<CourseListItem[] | null>(null)
  const [hasError, setHasError] = useState(false)
  const [removingBookmarkId, setRemovingBookmarkId] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    setCourses(null)
    setHasError(false)

    courseService.getCourses({ filter, scope })
      .then((data) => {
        if (!ignore) setCourses(data)
      })
      .catch(() => {
        if (!ignore) {
          setCourses([])
          setHasError(true)
        }
      })

    return () => {
      ignore = true
    }
  }, [filter, scope])

  const summary = useMemo(() => {
    const loadedCourses = courses ?? []
    return {
      count: loadedCourses.length,
      totalDistanceKm: loadedCourses.reduce((sum, course) => sum + course.distanceKm, 0),
      createdCount: loadedCourses.filter((course) => course.createdByMe).length,
    }
  }, [courses])

  const handleStart = (course: CourseListItem) => {
    navigate('/running/free', {
      state: {
        courseId: course.id,
        courseName: course.name,
        runningMode: 'COURSE_SELECT',
      },
    })
  }

  const handleRemoveBookmark = async (course: CourseListItem) => {
    if (!onRemoveBookmark || !course.bookmarkId) return
    setRemovingBookmarkId(course.bookmarkId)
    setHasError(false)
    try {
      await onRemoveBookmark(course.bookmarkId)
      setCourses((current) => current?.flatMap((item) => {
        if (item.id !== course.id) return [item]
        if (!item.createdByMe) return []
        return [{ ...item, bookmarkedByMe: false, bookmarkId: null }]
      }) ?? current)
    } catch {
      setHasError(true)
    } finally {
      setRemovingBookmarkId(null)
    }
  }

  return (
    <section className="course-library">
      {showHeader && (
        <div className="course-library-head">
          <div>
            <span>Jeju Running Course</span>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
          {showCreateAction && <Link to="/courses/create">새 코스</Link>}
        </div>
      )}

      <div className="course-library-summary" aria-label="코스 요약">
        <div><span>코스</span><strong>{summary.count}<small>개</small></strong></div>
        <div><span>총 거리</span><strong>{summary.totalDistanceKm.toFixed(1)}<small>km</small></strong></div>
        <div><span>내 코스</span><strong>{summary.createdCount}<small>개</small></strong></div>
      </div>

      <div className="course-library-filters" aria-label="코스 필터">
        {FILTER_OPTIONS.map((option) => (
          <button
            className={filter === option.value ? 'active' : ''}
            key={option.value}
            type="button"
            onClick={() => setFilter(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>

      {hasError && (
        <p className="course-library-error">코스 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>
      )}

      {courses === null ? (
        <div className="course-library-loading"><div className="spinner" /><span>코스를 불러오는 중이에요</span></div>
      ) : courses.length > 0 ? (
        <div className="course-library-list">
          {courses.map((course) => (
            <CourseListCard
              key={course.id}
              course={course}
              removingBookmarkId={removingBookmarkId}
              canRemoveBookmark={Boolean(onRemoveBookmark && course.bookmarkId)}
              onRemoveBookmark={() => handleRemoveBookmark(course)}
              onStart={() => handleStart(course)}
            />
          ))}
        </div>
      ) : (
        <div className="course-library-empty">
          <strong>{emptyTitle}</strong>
          <p>{emptyDescription}</p>
          {showCreateAction && <Link to="/courses/create">새 코스 만들기</Link>}
        </div>
      )}
    </section>
  )
}

function CourseListCard({
  course,
  removingBookmarkId,
  canRemoveBookmark,
  onRemoveBookmark,
  onStart,
}: {
  course: CourseListItem
  removingBookmarkId: string | null
  canRemoveBookmark: boolean
  onRemoveBookmark: () => void
  onStart: () => void
}) {
  const waypointPreview = course.waypointNames.length > 0
    ? course.waypointNames.slice(0, 3).join(' > ')
    : '경유지 정보 준비 중'
  const isRemovingBookmark = course.bookmarkId !== null && removingBookmarkId === course.bookmarkId

  return (
    <article className="course-library-card">
      <div className="course-library-card-image">
        <CourseRouteMap
          routeCoordinates={course.previewRouteCoordinates}
          waypoints={course.waypoints}
          className="course-library-card-map"
          fitTarget="planned"
          showCurrentPositionMarker={false}
          plannedRouteStyle={{ strokeWeight: 5 }}
        />
        <em>{courseTypeLabel[course.courseType]}</em>
      </div>
      <div className="course-library-card-body">
        <div className="course-library-card-badges">
          {course.createdByMe && <span>내가 만든 코스</span>}
          {!course.isPublic && <span>비공개</span>}
          {course.bookmarkedByMe && <span>저장됨</span>}
        </div>
        <h2>{course.name}</h2>
        {course.description && <p className="course-library-description">{course.description}</p>}
        <p className="course-library-waypoints">{waypointPreview}</p>
        <div className="course-library-stats">
          <span><b>{course.distanceKm.toFixed(1)}</b>km</span>
          <span><b>{course.estimatedDurationMinutes}</b>분</span>
          <span><b>{course.elevationGainM?.toFixed(0) ?? 0}</b>m</span>
          <span>{difficultyLabel[course.difficulty]}</span>
        </div>
        <div className={`course-library-actions ${canRemoveBookmark ? 'has-secondary' : ''}`}>
          {canRemoveBookmark && (
            <button type="button" disabled={isRemovingBookmark} onClick={onRemoveBookmark}>
              {isRemovingBookmark ? '해제 중' : '저장 해제'}
            </button>
          )}
          <Link to={`/courses/${course.id}`}>상세보기</Link>
          <button type="button" onClick={onStart}>이 코스로 달리기</button>
        </div>
      </div>
    </article>
  )
}
