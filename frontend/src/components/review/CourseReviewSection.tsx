import { useMemo, useState } from 'react'
import {
  createCourseReview,
  deleteCourseReview,
  updateCourseReview,
} from '../../api/courseReviews'
import type {
  CourseReview,
  CourseReviewCreateRequest,
  CourseReviewListResponse,
  CourseReviewUpdateRequest,
} from '../../types/courseReview'
import { RatingStars } from './RatingStars'
import { ReviewEditorSheet } from './ReviewEditorSheet'
import { ReviewSummaryPanel } from './ReviewSummaryPanel'

type CourseReviewSectionProps = {
  courseId: string
  errorMessage: string | null
  isLoading: boolean
  onRefresh: () => Promise<void>
  reviewsState: CourseReviewListResponse | null
  showPrimaryAction?: boolean
}

const DEMO_RUNNING_RECORD_ID = '00000000-0000-0000-0000-000000000001'

export function CourseReviewSection({
  courseId,
  errorMessage,
  isLoading,
  onRefresh,
  reviewsState,
  showPrimaryAction = true,
}: CourseReviewSectionProps) {
  const [editingReview, setEditingReview] = useState<CourseReview | null>(null)
  const [isEditorOpen, setIsEditorOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const myReview = useMemo(
    () => reviewsState?.reviews.find((review) => review.isMine) ?? null,
    [reviewsState],
  )

  const openCreateEditor = () => {
    setEditingReview(null)
    setSubmitError(null)
    setIsEditorOpen(true)
  }

  const openUpdateEditor = (review: CourseReview) => {
    setEditingReview(review)
    setSubmitError(null)
    setIsEditorOpen(true)
  }

  const closeEditor = () => {
    if (isSubmitting) {
      return
    }

    setEditingReview(null)
    setIsEditorOpen(false)
    setSubmitError(null)
  }

  const handleSubmit = async (payload: {
    content: string
    rating: number
    runningRecordId?: string
  }) => {
    setIsSubmitting(true)
    setSubmitError(null)

    try {
      if (editingReview) {
        const request: CourseReviewUpdateRequest = {
          content: payload.content,
          rating: payload.rating,
        }
        await updateCourseReview(courseId, editingReview.reviewId, request)
      } else {
        const request: CourseReviewCreateRequest = {
          content: payload.content,
          rating: payload.rating,
          runningRecordId: payload.runningRecordId || DEMO_RUNNING_RECORD_ID,
        }
        await createCourseReview(courseId, request)
      }

      await onRefresh()
      closeEditor()
    } catch (error) {
      setSubmitError(extractErrorMessage(error, '후기를 저장하지 못했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async (reviewId: string) => {
    setSubmitError(null)

    try {
      await deleteCourseReview(courseId, reviewId)
      await onRefresh()
    } catch (error) {
      setSubmitError(extractErrorMessage(error, '후기를 삭제하지 못했습니다.'))
    }
  }

  const reviews = reviewsState?.reviews ?? []
  const ratingAvg = formatRatingAvg(reviewsState?.ratingAvg)
  const reviewCount = reviewsState?.reviewCount ?? 0

  return (
    <section className="rounded-[28px] bg-white/92 p-5 shadow-[0_20px_60px_rgba(64,34,7,0.08)] ring-1 ring-[#f1dfd2] sm:p-7">
      <ReviewSummaryPanel
        hasMyReview={Boolean(myReview)}
        onWriteReview={openCreateEditor}
        ratingAvg={ratingAvg}
        reviewCount={reviewCount}
        showActionButton={showPrimaryAction}
      />

      {!showPrimaryAction ? (
        <button
          type="button"
          data-review-create-trigger="true"
          onClick={openCreateEditor}
          className="hidden"
          aria-hidden="true"
        >
          후기 작성 열기
        </button>
      ) : null}

      {submitError ? (
        <div className="mt-4 rounded-2xl border border-[#f4cfb6] bg-[#fff4ec] px-4 py-3 text-sm text-[#a25523]">
          {submitError}
        </div>
      ) : null}

      {errorMessage ? (
        <div className="mt-6 rounded-2xl border border-[#f4d2bf] bg-[#fff7f2] px-4 py-4 text-sm text-[#9d5b32]">
          {errorMessage}
        </div>
      ) : null}

      {!errorMessage && isLoading ? (
        <div className="mt-6 space-y-4">
          {[0, 1].map((index) => (
            <div
              key={index}
              className="h-36 animate-pulse rounded-[22px] bg-[#f6ede7]"
            />
          ))}
        </div>
      ) : null}

      {!errorMessage && !isLoading && reviews.length === 0 ? (
        <div className="mt-6 rounded-[24px] border border-dashed border-[#f0d8ca] bg-[#fffaf6] px-5 py-8 text-center">
          <p className="text-sm font-medium text-[#7a5b4a]">
            아직 등록된 후기가 없습니다.
          </p>
          <p className="mt-2 text-sm text-[#a28370]">
            코스를 완주한 러닝 기록이 있다면 첫 후기를 남길 수 있습니다.
          </p>
        </div>
      ) : null}

      {!errorMessage && !isLoading && reviews.length > 0 ? (
        <div className="mt-6 space-y-4">
          {reviews.map((review) => (
            <article
              key={review.reviewId}
              className="rounded-[24px] border border-[#f1dfd2] bg-[#fffaf7] px-4 py-4 shadow-[0_12px_30px_rgba(129,83,43,0.06)]"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex min-w-0 items-center gap-3">
                  {review.profileImageUrl ? (
                    <img
                      src={review.profileImageUrl}
                      alt=""
                      className="h-12 w-12 rounded-full object-cover"
                    />
                  ) : (
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#f7d7be] text-sm font-semibold text-[#a9541f]">
                      {getAvatarFallback(review.nickname)}
                    </div>
                  )}

                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="truncate text-sm font-semibold text-[#3b2517]">
                        {review.nickname}
                      </p>
                      {review.isMine ? (
                        <span className="rounded-full bg-[#ffeddc] px-2 py-0.5 text-[11px] font-semibold text-[#d96c24]">
                          내 후기
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-1 text-xs text-[#9c7b65]">
                      {formatDate(review.createdAt)}
                    </p>
                  </div>
                </div>

                <div className="shrink-0">
                  <RatingStars rating={review.rating} size="sm" />
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <InfoChip label="평점" value={formatOneDecimal(review.rating)} />
                <InfoChip
                  label="러닝 기록"
                  value={shortenId(review.runningRecordId)}
                />
              </div>

              <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-[#5a4030]">
                {review.content?.trim() || '후기 내용이 없습니다.'}
              </p>

              {review.isMine ? (
                <div className="mt-4 flex gap-2">
                  <button
                    type="button"
                    onClick={() => openUpdateEditor(review)}
                    className="rounded-full bg-[#fff0e4] px-4 py-2 text-sm font-semibold text-[#c56021] transition hover:bg-[#ffe4d0]"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(review.reviewId)}
                    className="rounded-full border border-[#edd1c0] px-4 py-2 text-sm font-semibold text-[#8b6044] transition hover:border-[#d9b49b] hover:bg-[#fff8f4]"
                  >
                    삭제
                  </button>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      ) : null}

      <ReviewEditorSheet
        defaultRunningRecordId={myReview?.runningRecordId ?? DEMO_RUNNING_RECORD_ID}
        initialReview={editingReview}
        isOpen={isEditorOpen}
        isSubmitting={isSubmitting}
        onClose={closeEditor}
        onSubmit={handleSubmit}
      />
    </section>
  )
}

function InfoChip({ label, value }: { label: string; value: string }) {
  return (
    <span className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1.5 text-xs text-[#91684d] ring-1 ring-[#f2dfd1]">
      <span className="font-semibold text-[#7f5336]">{label}</span>
      <span>{value}</span>
    </span>
  )
}

function extractErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = Reflect.get(error, 'response')

    if (typeof response === 'object' && response !== null && 'data' in response) {
      const data = Reflect.get(response, 'data')

      if (typeof data === 'string' && data.trim()) {
        return data
      }

      if (
        typeof data === 'object' &&
        data !== null &&
        'message' in data &&
        typeof Reflect.get(data, 'message') === 'string'
      ) {
        return Reflect.get(data, 'message') as string
      }
    }
  }

  return fallback
}

function formatDate(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date)
}

function formatRatingAvg(value?: number | string) {
  if (typeof value === 'number') {
    return value.toFixed(2)
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? value : parsed.toFixed(2)
  }

  return '0.00'
}

function formatOneDecimal(value: number) {
  return Number.isInteger(value) ? `${value}.0` : value.toFixed(1)
}

function getAvatarFallback(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || 'R'
}

function shortenId(value: string) {
  return `${value.slice(0, 8)}...`
}
