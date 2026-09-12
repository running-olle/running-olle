import { Icon, Badge, Button } from '../../components/ui'
import { useEffect, useState } from 'react'
import { CourseRouteMap } from '../course/CourseRouteMap'
import { courseService } from '../course/courseService'
import type { CourseDetail, CourseDifficulty, CourseType } from '../course/types'

type CoursePreviewModalProps = {
  courseId: string
  onClose: () => void
}

const courseTypeLabel: Record<CourseType, string> = {
  RUNNING_COURSE: '러닝 코스',
  SPOT_COURSE: '스팟 코스',
}

const difficultyLabel: Record<CourseDifficulty, string> = {
  LOW: '쉬움',
  MID: '보통',
  HIGH: '어려움',
}

export function CoursePreviewModal({ courseId, onClose }: CoursePreviewModalProps) {
  const [course, setCourse] = useState<CourseDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [hasError, setHasError] = useState(false)
  const [bookmarkPending, setBookmarkPending] = useState(false)

  useEffect(() => {
    let active = true
    setCourse(null)
    setLoading(true)
    setHasError(false)

    courseService.getCourse(courseId)
      .then((data) => {
        if (active) {
          setCourse(data)
        }
      })
      .catch(() => {
        if (active) {
          setHasError(true)
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [courseId])

  const toggleBookmark = async () => {
    if (!course || course.createdByMe || bookmarkPending) {
      return
    }

    setBookmarkPending(true)

    try {
      if (course.bookmarkedByMe) {
        await courseService.unbookmarkCourse(course.id)
        setCourse((current) => current ? { ...current, bookmarkedByMe: false, bookmarkId: null } : current)
      } else {
        const response = await courseService.bookmarkCourse(course.id)
        setCourse((current) => current ? { ...current, bookmarkedByMe: true, bookmarkId: response.bookmarkId } : current)
      }
    } catch {
      window.alert('코스 저장 상태를 바꾸지 못했습니다.')
    } finally {
      setBookmarkPending(false)
    }
  }

  const creatorName = course?.createdByMe ? '나' : course?.creatorNickname || '러닝올레 러너'
  const visibleWaypoints = course?.waypoints.slice(0, 4) ?? []

  return (
    <div
      className="ui-overlay ui-overlay--center community-course-preview-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="community-course-preview-title"
      onClick={onClose}
    >
      <section className="ui-dialog community-course-preview" onClick={(event) => event.stopPropagation()}>
        <Button variant="ghost" size="sm" type="button" className="community-course-preview-close" aria-label="닫기" onClick={onClose}>
          <Icon name="close" />
        </Button>

        {loading ? (
          <div className="community-course-preview-state">
            <div className="spinner" />
            <span>코스를 불러오는 중이에요</span>
          </div>
        ) : null}

        {!loading && hasError ? (
          <div className="community-course-preview-state">
            <strong>코스를 불러오지 못했어요</strong>
            <span>삭제되었거나 볼 수 없는 코스일 수 있어요.</span>
          </div>
        ) : null}

        {!loading && course ? (
          <>
            <div className="community-course-preview-title">
              <Badge variant={course.courseType === 'SPOT_COURSE' ? 'spot' : 'success'}>{courseTypeLabel[course.courseType]}</Badge>
              <h2 id="community-course-preview-title">{course.name}</h2>
              <p>{creatorName} · {course.isPublic ? '공개 코스' : '비공개 코스'}</p>
            </div>

            <CourseRouteMap
              routeCoordinates={course.routeCoordinates}
              waypoints={course.waypoints}
              className="community-course-preview-map"
              fitTarget="planned"
              showCurrentPositionMarker={false}
            />

            <div className="community-course-preview-stats" aria-label="코스 통계">
              <div><span>거리</span><strong>{course.distanceKm.toFixed(1)}<small>km</small></strong></div>
              <div><span>시간</span><strong>{course.estimatedDurationMinutes}<small>분</small></strong></div>
              <div><span>고도</span><strong>{course.elevationGainM?.toFixed(0) ?? 0}<small>m</small></strong></div>
              <div><span>난이도</span><strong>{difficultyLabel[course.difficulty]}</strong></div>
            </div>

            {course.description ? (
              <p className="community-course-preview-description">{course.description}</p>
            ) : null}

            <div className="community-course-preview-waypoints">
              <strong>경유지</strong>
              {visibleWaypoints.length > 0 ? (
                <ol>
                  {visibleWaypoints.map((waypoint, index) => (
                    <li key={waypoint.id ?? `${waypoint.name}-${index}`}>
                      <span>{index + 1}</span>
                      <p>{waypoint.name}</p>
                    </li>
                  ))}
                </ol>
              ) : (
                <p>저장된 경유지 정보가 없어요.</p>
              )}
              {course.waypoints.length > visibleWaypoints.length ? (
                <small>외 {course.waypoints.length - visibleWaypoints.length}곳 더 있어요.</small>
              ) : null}
            </div>

            <div className="community-course-preview-actions">
              {course.createdByMe ? (
                <Button variant="ghost" size="sm" type="button" className="is-muted" disabled>
                  내 코스
                </Button>
              ) : (
                <Button variant="secondary" size="sm" type="button" disabled={bookmarkPending} onClick={toggleBookmark}>
                  <Icon name="bookmark" fill={course.bookmarkedByMe || bookmarkPending ? 'currentColor' : 'none'} />
                  {bookmarkPending ? '처리 중' : course.bookmarkedByMe ? '저장 취소' : '저장하기'}
                </Button>
              )}
              <Button variant="primary" size="sm" type="button" onClick={onClose}>
                확인
              </Button>
            </div>
          </>
        ) : null}
      </section>
    </div>
  )
}
