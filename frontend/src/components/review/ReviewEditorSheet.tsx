import { useEffect, useState } from 'react'
import type { CourseReview } from '../../types/courseReview'
import { RatingStars } from './RatingStars'

type ReviewEditorSheetProps = {
  defaultRunningRecordId: string
  initialReview: CourseReview | null
  isOpen: boolean
  isSubmitting: boolean
  onClose: () => void
  onSubmit: (payload: {
    content: string
    rating: number
    runningRecordId?: string
  }) => Promise<void>
}

export function ReviewEditorSheet({
  defaultRunningRecordId,
  initialReview,
  isOpen,
  isSubmitting,
  onClose,
  onSubmit,
}: ReviewEditorSheetProps) {
  const [content, setContent] = useState('')
  const [rating, setRating] = useState(5)
  const [runningRecordId, setRunningRecordId] = useState(defaultRunningRecordId)

  useEffect(() => {
    if (!isOpen) {
      return
    }

    setContent(initialReview?.content ?? '')
    setRating(initialReview?.rating ?? 5)
    setRunningRecordId(initialReview?.runningRecordId ?? defaultRunningRecordId)
  }, [defaultRunningRecordId, initialReview, isOpen])

  if (!isOpen) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-[#2a160b]/40 px-4 py-6 backdrop-blur-[2px]">
      <div className="w-full max-w-xl rounded-[30px] bg-[#fffaf7] px-5 pb-6 pt-4 shadow-[0_26px_80px_rgba(50,27,11,0.22)] sm:px-6">
        <div className="mx-auto h-1.5 w-14 rounded-full bg-[#edd7c8]" />

        <div className="mt-5 flex items-start justify-between gap-4">
          <div>
            <p className="text-lg font-semibold text-[#2e1d12]">
              {initialReview ? '후기 수정' : '후기 작성'}
            </p>
            <p className="mt-1 text-sm text-[#9f7d69]">
              코스를 실제로 달린 기록을 기준으로 후기를 남깁니다.
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-full bg-white px-3 py-2 text-sm font-semibold text-[#8e6344] ring-1 ring-[#f0dfd4] transition hover:bg-[#fff2e7]"
          >
            닫기
          </button>
        </div>

        <form
          className="mt-6 space-y-5"
          onSubmit={async (event) => {
            event.preventDefault()
            await onSubmit({ content, rating, runningRecordId })
          }}
        >
          {!initialReview ? (
            <label className="block">
              <span className="mb-2 block text-sm font-semibold text-[#5d4030]">
                러닝 기록 ID
              </span>
              <input
                value={runningRecordId}
                onChange={(event) => setRunningRecordId(event.target.value)}
                placeholder="완주한 러닝 기록 UUID"
                className="w-full rounded-2xl border border-[#eeded2] bg-white px-4 py-3 text-sm text-[#3d281a] outline-none transition focus:border-[#ff9c5a] focus:ring-4 focus:ring-[#ffd9bc]"
              />
            </label>
          ) : (
            <div className="rounded-2xl bg-white px-4 py-3 ring-1 ring-[#f0dfd4]">
              <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#b0886f]">
                연결된 러닝 기록
              </p>
              <p className="mt-2 break-all text-sm text-[#5b4132]">
                {initialReview.runningRecordId}
              </p>
            </div>
          )}

          <div>
            <span className="mb-3 block text-sm font-semibold text-[#5d4030]">
              평점
            </span>
            <div className="flex items-center justify-between rounded-2xl bg-white px-4 py-3 ring-1 ring-[#f0dfd4]">
              <RatingStars
                interactive
                onChange={setRating}
                rating={rating}
                size="lg"
              />
              <span className="text-sm font-semibold text-[#da6a22]">
                {formatRating(rating)} / 5.0
              </span>
            </div>
          </div>

          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-[#5d4030]">
              후기 내용
            </span>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={5}
              placeholder="코스 난이도, 풍경, 러닝 리듬, 다시 뛰고 싶은 포인트를 적어보세요."
              className="w-full resize-none rounded-2xl border border-[#eeded2] bg-white px-4 py-3 text-sm leading-6 text-[#3d281a] outline-none transition focus:border-[#ff9c5a] focus:ring-4 focus:ring-[#ffd9bc]"
            />
          </label>

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex w-full items-center justify-center rounded-full bg-[#ff7e33] px-4 py-3 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(255,126,51,0.32)] transition hover:bg-[#f06e20] disabled:cursor-not-allowed disabled:bg-[#f8b184]"
          >
            {isSubmitting
              ? '저장 중...'
              : initialReview
                ? '후기 수정 저장'
                : '후기 등록'}
          </button>
        </form>
      </div>
    </div>
  )
}

function formatRating(value: number) {
  return Number.isInteger(value) ? `${value}.0` : value.toFixed(1)
}
