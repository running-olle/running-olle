import { useEffect, useState } from 'react'
import { Button, Icon, Textarea } from '../../components/ui'
import type { CourseReviewInput } from './types'

type CourseReviewEditorProps = {
  initialRating?: number
  initialContent?: string | null
  submitLabel?: string
  busy?: boolean
  error?: string
  onCancel?: () => void
  onSubmit: (input: CourseReviewInput) => void | Promise<void>
}

export function CourseReviewEditor({
  initialRating = 0,
  initialContent = '',
  submitLabel = '리뷰 등록',
  busy = false,
  error = '',
  onCancel,
  onSubmit,
}: CourseReviewEditorProps) {
  const [rating, setRating] = useState(initialRating)
  const [content, setContent] = useState(initialContent ?? '')

  useEffect(() => {
    setRating(initialRating)
    setContent(initialContent ?? '')
  }, [initialContent, initialRating])

  return (
    <div className="course-review-editor">
      <fieldset className="course-review-rating">
        <legend>코스는 어떠셨나요?</legend>
        <div role="radiogroup" aria-label="별점 선택">
          {[1, 2, 3, 4, 5].map((value) => (
            <button
              key={value}
              type="button"
              role="radio"
              aria-checked={rating === value}
              aria-label={`${value}점`}
              className={value <= rating ? 'is-selected' : ''}
              disabled={busy}
              onClick={() => setRating(value)}
            >
              <Icon name="star" size={30} fill={value <= rating ? 'currentColor' : 'none'} />
            </button>
          ))}
        </div>
        <span>{rating > 0 ? `${rating}점` : '별점을 선택해 주세요'}</span>
      </fieldset>
      <Textarea
        label="한 줄 후기 (선택)"
        value={content}
        maxLength={1000}
        count={`${content.length}/1000`}
        placeholder="이 코스를 달리며 느낀 점을 남겨주세요."
        disabled={busy}
        onChange={(event) => setContent(event.target.value)}
      />
      {error && <p className="course-review-error" role="alert">{error}</p>}
      <div className="course-review-editor-actions">
        {onCancel && <Button variant="secondary" disabled={busy} onClick={onCancel}>취소</Button>}
        <Button variant="primary" loading={busy} disabled={rating === 0} onClick={() => onSubmit({ rating, content: content.trim() || null })}>{submitLabel}</Button>
      </div>
    </div>
  )
}

export function reviewErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const message = (error as { response?: { data?: { message?: string; detail?: string } } }).response?.data
    return message?.message || message?.detail || '리뷰를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.'
  }
  return '리뷰를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.'
}
