import { Icon, Chip, Button } from '../../components/ui'
import { FullScreenPage } from '../../components/layout/FullScreenPage'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  createFeedPost,
  getFeedCourseOptions,
  getFeedRunningRecordOptions,
  updateFeedPost,
  uploadFeedImages,
  type FeedPost,
  type FeedSelectionOption,
  type FeedVisibility,
} from './api'

type FeedComposerProps = {
  editingPost?: FeedPost | null
  onCancel: () => void
  onCreated: (post: FeedPost) => void
}

export function FeedComposer({ editingPost, onCancel, onCreated }: FeedComposerProps) {
  const isEditMode = !!editingPost
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const [content, setContent] = useState(editingPost?.content ?? '')
  const [visibility, setVisibility] = useState<FeedVisibility>(editingPost?.visibility ?? 'PUBLIC')
  const [photoTagged, setPhotoTagged] = useState(editingPost?.photoTagged ?? false)
  const [selectedImages, setSelectedImages] = useState<string[]>(editingPost?.imageUrls ?? [])
  const [runningRecordOptions, setRunningRecordOptions] = useState<FeedSelectionOption[]>([])
  const [courseOptions, setCourseOptions] = useState<FeedSelectionOption[]>([])
  const [selectedRunningRecordId, setSelectedRunningRecordId] = useState(editingPost?.runningRecord?.id ?? '')
  const [selectedCourseId, setSelectedCourseId] = useState(editingPost?.course?.id ?? '')
  const [loadingOptions, setLoadingOptions] = useState(true)
  const [uploadingImages, setUploadingImages] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const remainingCount = useMemo(() => 10 - selectedImages.length, [selectedImages.length])
  const selectedRunningRecord = runningRecordOptions.find((item) => item.id === selectedRunningRecordId) ?? null
  const selectedCourse = courseOptions.find((item) => item.id === selectedCourseId) ?? null

  useEffect(() => {
    let active = true
    setLoadingOptions(true)

    Promise.all([getFeedRunningRecordOptions(), getFeedCourseOptions()])
      .then(([runningRecords, courses]) => {
        if (!active) return
        setRunningRecordOptions(runningRecords)
        setCourseOptions(courses)
      })
      .catch(() => {
        if (!active) return
        setError('러닝 기록 또는 코스 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (active) setLoadingOptions(false)
      })

    return () => {
      active = false
    }
  }, [])

  const openFilePicker = () => {
    if (remainingCount <= 0 || uploadingImages) {
      return
    }
    fileInputRef.current?.click()
  }

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? [])
    const availableSlots = Math.max(0, 10 - selectedImages.length)
    const limitedFiles = files.slice(0, availableSlots)

    if (limitedFiles.length === 0) {
      setError('사진은 최대 10장까지 업로드할 수 있습니다.')
      event.target.value = ''
      return
    }

    if (files.length > limitedFiles.length) {
      setError(`사진은 최대 10장까지 업로드할 수 있어 ${limitedFiles.length}장만 추가됩니다.`)
    }

    setUploadingImages(true)
    setError((current) => (current === '이미지 업로드에 실패했습니다.' ? '' : current))

    try {
      const imageUrls = await uploadFeedImages(limitedFiles)
      setSelectedImages((prev) => [...prev, ...imageUrls])
    } catch {
      setError('이미지 업로드에 실패했습니다.')
    } finally {
      setUploadingImages(false)
      event.target.value = ''
    }
  }

  const removeImage = (index: number) => {
    setSelectedImages((prev) => prev.filter((_, currentIndex) => currentIndex !== index))
  }

  const submit = async () => {
    if (!content.trim()) {
      setError('내용을 입력해 주세요.')
      return
    }

    if (selectedImages.length > 10) {
      setError('사진은 최대 10장까지 등록할 수 있습니다.')
      return
    }

    setSubmitting(true)
    setError('')

    try {
      const payload = {
        runningRecordId: selectedRunningRecordId || null,
        courseId: selectedCourseId || null,
        content: content.trim(),
        visibility,
        region: '제주',
        photoTagged,
        imageUrls: selectedImages,
      }

      const post =
        isEditMode && editingPost ? await updateFeedPost(editingPost.id, payload) : await createFeedPost(payload)

      onCreated(post)
    } catch {
      setError(isEditMode ? '게시글을 수정하지 못했습니다.' : '게시글을 등록하지 못했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="community-backdrop">
      <FullScreenPage scroll={false} role="dialog" aria-modal="true" aria-label="게시글 작성" className="community-dialog">
        <div className="community-dialog-header">
          <Button variant="secondary" size="sm"
            type="button"
            onClick={onCancel}
            className="rounded-full border border-border-subtle px-4 py-2 text-label font-bold text-ink-secondary"
          >
            취소
          </Button>
          <strong className="text-card-title font-bold text-ink">
            {isEditMode ? '게시글 수정' : '게시글 작성'}
          </strong>
          <Button variant="primary" size="sm"
            type="button"
            onClick={submit}
            disabled={submitting || uploadingImages}
            className="rounded-full bg-brand-500 px-4 py-2 text-label font-bold text-surface disabled:opacity-50"
          >
            {submitting ? (isEditMode ? '수정 중' : '등록 중') : isEditMode ? '수정' : '게시'}
          </Button>
        </div>

        <div className="community-dialog-body flex-1 overflow-y-auto px-5 py-5">
          <div className="grid gap-4">
            <OptionSection
              title="러닝 기록 연결"
              helper="실제 러닝 기록을 연결하면 거리와 페이스가 함께 노출됩니다."
              loading={loadingOptions}
              options={runningRecordOptions}
              selectedId={selectedRunningRecordId}
              onChange={setSelectedRunningRecordId}
              emptyLabel="연결하지 않음"
              disabled={isEditMode}
            />

            <OptionSection
              title="코스 태그 선택"
              helper="공개 코스를 연결하면 피드에서 바로 보이도록 붙습니다."
              loading={loadingOptions}
              options={courseOptions}
              selectedId={selectedCourseId}
              onChange={setSelectedCourseId}
              emptyLabel="선택 안 함"
            />
          </div>

          <div className="mt-5">
            <label className="mb-2 block text-label font-bold text-ink">내용</label>
            <textarea aria-label="오늘의 러닝 기록이나 제주에서의 경험을 남겨 보세요."
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="오늘의 러닝 기록이나 제주에서의 경험을 남겨 보세요."
              className="ui-textarea min-h-[150px] w-full rounded-control border border-border-subtle bg-surface px-4 py-3 text-body-sm leading-6 text-ink outline-none"
            />
          </div>

          <div className="mt-5">
            <div className="mb-2 flex items-center justify-between">
              <label className="block text-label font-bold text-ink">사진</label>
              <span className="text-caption text-ink-secondary">{selectedImages.length} / 10</span>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              className="hidden"
              onChange={handleFileChange}
            />
            <div className="flex gap-3 overflow-x-auto pb-1">
              <Button variant="secondary" size="sm"
                type="button"
                onClick={openFilePicker}
                disabled={remainingCount <= 0 || uploadingImages}
                className="community-image-add flex h-18 w-18 shrink-0 flex-col items-center justify-center rounded-control border border-dashed border-border-subtle bg-surface text-caption font-bold text-ink-secondary disabled:opacity-40"
              >
                <span className="text-section-title leading-none">+</span>
                {uploadingImages ? '업로드 중' : '추가'}
              </Button>
              {selectedImages.map((imageUrl, index) => (
                <button
                  key={`${imageUrl}-${index}`}
                  type="button"
                  onClick={() => removeImage(index)}
                  className="relative h-18 w-18 shrink-0 overflow-hidden rounded-control bg-cover bg-center"
                  style={{ backgroundImage: `url(${imageUrl})` }}
                  aria-label="이미지 제거"
                >
                  <span className="absolute right-1 top-1 rounded-full bg-(--color-overlay) px-1.5 text-caption text-surface">
                    <Icon name="close" />
                  </span>
                </button>
              ))}
            </div>
          </div>

          <div className="mt-5 grid grid-cols-2 gap-3">
            <ToggleChoice label="전체 공개" active={visibility === 'PUBLIC'} onClick={() => setVisibility('PUBLIC')} />
            <ToggleChoice label="비공개" active={visibility === 'PRIVATE'} onClick={() => setVisibility('PRIVATE')} />
          </div>

          <button
            type="button"
            onClick={() => setPhotoTagged((prev) => !prev)}
            className={`mt-4 flex w-full items-center justify-between rounded-control border px-4 py-3 text-left ${
              photoTagged ? 'border-brand-500 bg-surface-subtle' : 'border-border-subtle bg-surface'
            }`}
          >
            <div>
              <div className="text-label font-bold text-ink">포토 태그</div>
              <div className="mt-1 text-caption text-ink-secondary">
                사진 중심 피드 필터에서 바로 노출됩니다.
              </div>
            </div>
            <span className="text-caption font-bold text-brand-700">{photoTagged ? 'ON' : 'OFF'}</span>
          </button>

          {(selectedRunningRecord || selectedCourse) && !loadingOptions ? (
            <div className="mt-4 rounded-control bg-surface-subtle p-4 text-caption text-ink-secondary">
              {selectedRunningRecord ? (
                <div>
                  러닝 기록: {selectedRunningRecord.label}
                  {selectedRunningRecord.distanceKm ? ` · ${selectedRunningRecord.distanceKm.toFixed(1)}km` : ''}
                </div>
              ) : null}
              {selectedCourse ? (
                <div className={selectedRunningRecord ? 'mt-2' : ''}>
                  코스 태그: {selectedCourse.label} ·{' '}
                  {selectedCourse.courseType === 'RUNNING_COURSE' ? '러닝 코스' : '스팟 코스'}
                </div>
              ) : null}
            </div>
          ) : null}

          {error ? <p className="mt-4 rounded-control bg-danger-subtle px-4 py-3 text-caption text-danger">{error}</p> : null}
        </div>
      </FullScreenPage>
    </div>
  )
}

function OptionSection({
  title,
  helper,
  loading,
  options,
  selectedId,
  onChange,
  emptyLabel,
  disabled = false,
}: {
  title: string
  helper: string
  loading: boolean
  options: FeedSelectionOption[]
  selectedId: string
  onChange: (value: string) => void
  emptyLabel: string
  disabled?: boolean
}) {
  return (
    <div className="rounded-md bg-surface-subtle p-4 shadow-none">
      <div className="text-body-sm font-bold text-ink">{title}</div>
      <div className="mt-1 text-caption text-ink-secondary">{helper}</div>
      <select aria-label={title}
        value={selectedId}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled}
        className="ui-input mt-3 w-full rounded-control border border-border-subtle bg-surface px-4 py-3 text-label text-ink outline-none disabled:opacity-60"
      >
        <option value="">{loading ? '불러오는 중...' : emptyLabel}</option>
        {options.map((option) => (
          <option key={option.id} value={option.id}>
            {buildOptionLabel(option)}
          </option>
        ))}
      </select>
    </div>
  )
}

function ToggleChoice({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return <Chip variant="choice" selected={active} onClick={onClick}>{label}</Chip>
}

function buildOptionLabel(option: FeedSelectionOption) {
  const parts = [option.label]
  if (option.courseType === 'RUNNING_COURSE') parts.push('러닝 코스')
  if (option.courseType === 'SPOT_COURSE') parts.push('스팟 코스')
  if (option.distanceKm) parts.push(`${option.distanceKm.toFixed(1)}km`)
  return parts.join(' · ')
}
