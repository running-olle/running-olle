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
  kicker?: string
  createActionLabel?: string
  createdBadgeLabel?: string
  onRemoveBookmark?: (bookmarkId: string) => Promise<void>
  onDeleteCourse?: (courseId: string) => Promise<void>
  showHeader?: boolean
  showCreateAction?: boolean
  showCreatedFilter?: boolean
  showSummary?: boolean
  showSearch?: boolean
  showDescription?: boolean
  compactCards?: boolean
  showStartAction?: boolean
  showBookmarkAction?: boolean
  summaryThirdMetric?: 'CREATED' | 'BOOKMARKED'
}

export function CourseListView({
  scope,
  title,
  subtitle,
  emptyTitle,
  emptyDescription,
  kicker = 'Jeju Running Course',
  createActionLabel = '새 코스',
  createdBadgeLabel = '내가 만든 코스',
  onRemoveBookmark,
  onDeleteCourse,
  showHeader = true,
  showCreateAction = true,
  showCreatedFilter = true,
  showSummary = true,
  showSearch = false,
  showDescription = true,
  compactCards = false,
  showStartAction = true,
  showBookmarkAction = false,
  summaryThirdMetric = 'CREATED',
}: CourseListViewProps) {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<CourseListFilter>('ALL')
  const [courses, setCourses] = useState<CourseListItem[] | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [hasError, setHasError] = useState(false)
  const [removingBookmarkId, setRemovingBookmarkId] = useState<string | null>(null)
  const [savingBookmarkCourseId, setSavingBookmarkCourseId] = useState<string | null>(null)
  const [deletingCourseId, setDeletingCourseId] = useState<string | null>(null)

  const filterOptions = useMemo(() => (
    showCreatedFilter
      ? FILTER_OPTIONS
      : FILTER_OPTIONS.filter((option) => option.value !== 'CREATED')
  ), [showCreatedFilter])

  useEffect(() => {
    if (!showCreatedFilter && filter === 'CREATED') {
      setFilter('ALL')
    }
  }, [filter, showCreatedFilter])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setKeyword(searchInput.trim())
    }, 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    let ignore = false
    setCourses(null)
    setHasError(false)

    courseService.getCourses({ filter, scope, keyword })
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
  }, [filter, keyword, scope])

  const summary = useMemo(() => {
    const loadedCourses = courses ?? []
    return {
      count: loadedCourses.length,
      totalDistanceKm: loadedCourses.reduce((sum, course) => sum + course.distanceKm, 0),
      createdCount: loadedCourses.filter((course) => course.createdByMe).length,
      bookmarkedCount: loadedCourses.filter((course) => course.bookmarkedByMe).length,
    }
  }, [courses])

  const thirdSummary = summaryThirdMetric === 'BOOKMARKED'
    ? { label: '저장됨', value: summary.bookmarkedCount }
    : { label: '내 코스', value: summary.createdCount }

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

  const handleToggleBookmark = async (course: CourseListItem) => {
    if (course.createdByMe) return
    setSavingBookmarkCourseId(course.id)
    setHasError(false)
    try {
      if (course.bookmarkedByMe) {
        await courseService.unbookmarkCourse(course.id)
        setCourses((current) => current?.map((item) => (
          item.id === course.id
            ? { ...item, bookmarkedByMe: false, bookmarkId: null }
            : item
        )) ?? current)
      } else {
        const response = await courseService.bookmarkCourse(course.id)
        setCourses((current) => current?.map((item) => (
          item.id === course.id
            ? { ...item, bookmarkedByMe: true, bookmarkId: response.bookmarkId }
            : item
        )) ?? current)
      }
    } catch {
      setHasError(true)
    } finally {
      setSavingBookmarkCourseId(null)
    }
  }

  const handleDeleteCourse = async (course: CourseListItem) => {
    if (!onDeleteCourse || !course.createdByMe) return
    const confirmed = window.confirm('이 코스를 삭제할까요? 저장한 다른 사용자에게도 더 이상 보이지 않아요.')
    if (!confirmed) return

    setDeletingCourseId(course.id)
    setHasError(false)
    try {
      await onDeleteCourse(course.id)
      setCourses((current) => current?.filter((item) => item.id !== course.id) ?? current)
    } catch {
      setHasError(true)
    } finally {
      setDeletingCourseId(null)
    }
  }

  return (
    <section className={`course-library ${compactCards ? 'is-compact' : ''}`}>
      {showHeader && (
        <div className="course-library-head">
          <div>
            <span>{kicker}</span>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
          {showCreateAction && <Link to="/courses/create">{createActionLabel}</Link>}
        </div>
      )}

      {showSearch && (
        <label className="course-library-search">
          <span aria-hidden="true">
            <SearchIcon />
          </span>
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="코스명이나 스팟으로 검색"
          />
          {searchInput && (
            <button type="button" aria-label="검색어 지우기" onClick={() => setSearchInput('')}>
              ×
            </button>
          )}
        </label>
      )}

      {showSummary && (
        <div className="course-library-summary" aria-label="코스 요약">
          <div><span>코스</span><strong>{summary.count}<small>개</small></strong></div>
          <div><span>총 거리</span><strong>{summary.totalDistanceKm.toFixed(1)}<small>km</small></strong></div>
          <div><span>{thirdSummary.label}</span><strong>{thirdSummary.value}<small>개</small></strong></div>
        </div>
      )}

      <div className="course-library-filters" aria-label="코스 필터">
        {filterOptions.map((option) => (
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
              savingBookmarkCourseId={savingBookmarkCourseId}
              deletingCourseId={deletingCourseId}
              canRemoveBookmark={Boolean(onRemoveBookmark && course.bookmarkId)}
              canDeleteCourse={Boolean(onDeleteCourse && course.createdByMe)}
              createdBadgeLabel={createdBadgeLabel}
              showStartAction={showStartAction}
              showBookmarkAction={showBookmarkAction}
              showDescription={showDescription}
              onRemoveBookmark={() => handleRemoveBookmark(course)}
              onDeleteCourse={() => handleDeleteCourse(course)}
              onToggleBookmark={() => handleToggleBookmark(course)}
              onStart={() => handleStart(course)}
            />
          ))}
        </div>
      ) : (
        <div className="course-library-empty">
          <strong>{emptyTitle}</strong>
          <p>{emptyDescription}</p>
          {showCreateAction && <Link to="/courses/create">{createActionLabel}</Link>}
        </div>
      )}
    </section>
  )
}

function CourseListCard({
  course,
  removingBookmarkId,
  savingBookmarkCourseId,
  deletingCourseId,
  canRemoveBookmark,
  canDeleteCourse,
  createdBadgeLabel,
  showStartAction,
  showBookmarkAction,
  showDescription,
  onRemoveBookmark,
  onDeleteCourse,
  onToggleBookmark,
  onStart,
}: {
  course: CourseListItem
  removingBookmarkId: string | null
  savingBookmarkCourseId: string | null
  deletingCourseId: string | null
  canRemoveBookmark: boolean
  canDeleteCourse: boolean
  createdBadgeLabel: string
  showStartAction: boolean
  showBookmarkAction: boolean
  showDescription: boolean
  onRemoveBookmark: () => void
  onDeleteCourse: () => void
  onToggleBookmark: () => void
  onStart: () => void
}) {
  const waypointPreview = course.waypointNames.length > 0
    ? course.waypointNames.slice(0, 3).join(' > ')
    : '경유지 정보 준비 중'
  const isRemovingBookmark = course.bookmarkId !== null && removingBookmarkId === course.bookmarkId
  const isSavingBookmark = savingBookmarkCourseId === course.id
  const isDeletingCourse = deletingCourseId === course.id
  const showBookmarkButton = showBookmarkAction && !course.createdByMe
  const actionCount = [
    canRemoveBookmark && !course.createdByMe,
    canDeleteCourse,
    true,
    showStartAction,
  ].filter(Boolean).length

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
        {showBookmarkButton && (
          <button
            className={`course-library-bookmark ${course.bookmarkedByMe ? 'is-saved' : ''}`}
            type="button"
            aria-label={`${course.name} ${course.bookmarkedByMe ? '저장됨' : '저장하기'}`}
            title={course.bookmarkedByMe ? '저장됨' : '저장하기'}
            disabled={isSavingBookmark}
            onClick={onToggleBookmark}
          >
            <BookmarkIcon filled={course.bookmarkedByMe || isSavingBookmark} />
          </button>
        )}
      </div>
      <div className="course-library-card-body">
        <div className="course-library-card-badges">
          {course.createdByMe && <span>{createdBadgeLabel}</span>}
          {!course.isPublic && <span>비공개</span>}
          {course.bookmarkedByMe && <span>저장됨</span>}
        </div>
        <h2>{course.name}</h2>
        {showDescription && course.description && <p className="course-library-description">{course.description}</p>}
        <p className="course-library-waypoints">{waypointPreview}</p>
        <div className="course-library-stats">
          <span><b>{course.distanceKm.toFixed(1)}</b>km</span>
          <span><b>{course.estimatedDurationMinutes}</b>분</span>
          <span><b>{course.elevationGainM?.toFixed(0) ?? 0}</b>m</span>
          <span>{difficultyLabel[course.difficulty]}</span>
        </div>
        <div className="course-library-actions" data-count={actionCount}>
          {canRemoveBookmark && !course.createdByMe && (
            <button className="course-library-action is-secondary" type="button" disabled={isRemovingBookmark} onClick={onRemoveBookmark}>
              {isRemovingBookmark ? '해제 중' : '저장 해제'}
            </button>
          )}
          {canDeleteCourse && (
            <button className="course-library-action is-danger" type="button" disabled={isDeletingCourse} onClick={onDeleteCourse}>
              {isDeletingCourse ? '삭제 중' : '코스 삭제'}
            </button>
          )}
          <Link className="course-library-action is-secondary" to={`/courses/${course.id}`}>상세보기</Link>
          {showStartAction && (
            <button className="course-library-action is-primary" type="button" onClick={onStart}>
              이 코스로 달리기
            </button>
          )}
        </div>
      </div>
    </article>
  )
}

function BookmarkIcon({ filled }: { filled: boolean }) {
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

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  )
}
