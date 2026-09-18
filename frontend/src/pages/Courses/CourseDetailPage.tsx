import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { CourseRouteMap } from '../../features/course/CourseRouteMap'
import { courseService } from '../../features/course/courseService'
import { CourseReviewEditor, reviewErrorMessage } from '../../features/course/CourseReviewEditor'
import type { CourseDetail, CourseDifficulty, CourseReview, CourseReviewInput, CourseType } from '../../features/course/types'
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
  const [isReviewsOpen, setIsReviewsOpen] = useState(false)
  const [reviews, setReviews] = useState<CourseReview[] | null>(null)
  const [reviewsError, setReviewsError] = useState(false)
  const [editingReviewId, setEditingReviewId] = useState<string | null>(null)
  const [reviewSaving, setReviewSaving] = useState(false)
  const [reviewMutationError, setReviewMutationError] = useState('')

  useEffect(() => {
    if (!courseId) return
    let ignore = false
    setCourse(null)
    setHasError(false)
    setBookmarkError(false)
    setReviews(null)
    setReviewsError(false)

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

    courseService.getReviews(courseId)
      .then((data) => {
        if (!ignore) setReviews(data)
      })
      .catch(() => {
        if (!ignore) {
          setReviews([])
          setReviewsError(true)
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

  const updateReview = async (reviewId: string, input: CourseReviewInput) => {
    if (!course || reviewSaving) return
    setReviewSaving(true)
    setReviewMutationError('')
    try {
      const updated = await courseService.updateReview(course.id, reviewId, input)
      setReviews((current) => current?.map((review) => review.id === reviewId ? updated : review) ?? [updated])
      setCourse((current) => current ? { ...current, ratingAvg: averageRating(reviews?.map((review) => review.id === reviewId ? updated : review) ?? [updated]) } : current)
      setEditingReviewId(null)
    } catch (error) {
      setReviewMutationError(reviewErrorMessage(error))
    } finally {
      setReviewSaving(false)
    }
  }

  const deleteReview = async (reviewId: string) => {
    if (!course || reviewSaving || !window.confirm('이 리뷰를 삭제할까요?')) return
    setReviewSaving(true)
    setReviewMutationError('')
    try {
      await courseService.deleteReview(course.id, reviewId)
      const remaining = reviews?.filter((review) => review.id !== reviewId) ?? []
      setReviews(remaining)
      setCourse((current) => current ? { ...current, ratingAvg: averageRating(remaining) } : current)
      setEditingReviewId(null)
      if (remaining.length === 0) setIsReviewsOpen(false)
    } catch (error) {
      setReviewMutationError(reviewErrorMessage(error))
    } finally {
      setReviewSaving(false)
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
        {course.themes.map((theme) => <Badge variant="neutral" key={theme.id}>{theme.name}</Badge>)}
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

      <section className="course-review-section">
        <div className="course-review-section-heading">
          <div>
            <h2>러너 리뷰</h2>
            <p>직접 이 코스를 달린 러너들의 후기예요.</p>
          </div>
          <strong><Icon name="star" size={18} fill="currentColor" />{course.ratingAvg.toFixed(1)}<small>({reviews?.length ?? 0})</small></strong>
        </div>
        {reviews === null ? (
          <div className="course-review-loading"><Spinner label="리뷰를 불러오는 중" /></div>
        ) : reviewsError ? (
          <p className="course-review-empty">리뷰를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.</p>
        ) : reviews.length === 0 ? (
          <p className="course-review-empty">아직 등록된 리뷰가 없어요. 이 코스를 달리고 첫 리뷰를 남겨보세요.</p>
        ) : (
          <>
            <div className="course-review-latest-label">가장 최근 리뷰</div>
            <ReviewCard
              review={reviews[0]}
              editing={editingReviewId === reviews[0].id}
              busy={reviewSaving}
              error={reviewMutationError}
              onEdit={() => { setEditingReviewId(reviews[0].id); setReviewMutationError('') }}
              onCancel={() => { setEditingReviewId(null); setReviewMutationError('') }}
              onUpdate={(input) => updateReview(reviews[0].id, input)}
              onDelete={() => deleteReview(reviews[0].id)}
            />
            <Button className="course-review-view-all" variant="secondary" fullWidth onClick={() => setIsReviewsOpen(true)}>
              리뷰 전체보기 ({reviews.length})
            </Button>
          </>
        )}
      </section>

      <Modal
        open={isReviewsOpen}
        title="러너 리뷰 전체보기"
        onClose={() => { setIsReviewsOpen(false); setEditingReviewId(null); setReviewMutationError('') }}
        className="course-review-modal"
      >
        <div className="course-review-list">
          {reviews?.map((review) => (
            <ReviewCard
              key={review.id}
              review={review}
              editing={editingReviewId === review.id}
              busy={reviewSaving}
              error={reviewMutationError}
              onEdit={() => { setEditingReviewId(review.id); setReviewMutationError('') }}
              onCancel={() => { setEditingReviewId(null); setReviewMutationError('') }}
              onUpdate={(input) => updateReview(review.id, input)}
              onDelete={() => deleteReview(review.id)}
            />
          ))}
        </div>
      </Modal>

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

function formatReviewDate(value: string) {
  return new Date(value).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function averageRating(reviews: CourseReview[]) {
  if (reviews.length === 0) return 0
  return reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length
}

function ReviewCard({
  review,
  editing,
  busy,
  error,
  onEdit,
  onCancel,
  onUpdate,
  onDelete,
}: {
  review: CourseReview
  editing: boolean
  busy: boolean
  error: string
  onEdit: () => void
  onCancel: () => void
  onUpdate: (input: CourseReviewInput) => void | Promise<void>
  onDelete: () => void
}) {
  return (
    <article className="course-review-card">
      <header>
        <span>{(review.userNickname || '러너').slice(0, 1)}</span>
        <div><strong>{review.authoredByMe ? '나' : review.userNickname || '러닝올레 러너'}</strong><small>{formatReviewDate(review.createdAt)}</small></div>
        <b aria-label={`별점 ${review.rating}점`}><Icon name="star" size={16} fill="currentColor" />{review.rating.toFixed(1)}</b>
      </header>
      {editing ? (
        <CourseReviewEditor
          initialRating={review.rating}
          initialContent={review.content}
          submitLabel="수정 완료"
          busy={busy}
          error={error}
          onCancel={onCancel}
          onSubmit={onUpdate}
        />
      ) : (
        <>
          <p>{review.content || '별점으로 코스를 평가했어요.'}</p>
          {review.authoredByMe && (
            <div className="course-review-actions">
              <button type="button" onClick={onEdit}>수정</button>
              <button type="button" disabled={busy} onClick={onDelete}>삭제</button>
            </div>
          )}
        </>
      )}
    </article>
  )
}
