import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { courseBuilderService } from '../../features/courseBuilder/courseBuilderService'
import { useCourseDraftStore } from '../../features/courseBuilder/courseDraftStore'
import { difficultyLabel, formatDistanceKm } from '../../features/courseBuilder/courseBuilderUtils'
import type { CourseTagOption, CourseType, ThemeOption } from '../../features/courseBuilder/types'
import { BottomSheet, Button, Chip, Icon, IconButton, Input, SectionHeader, Switch, Textarea } from '../../components/ui'

type LoadStatus = 'idle' | 'loading' | 'success' | 'error'
type SubmitStatus = 'idle' | 'saving' | 'success' | 'error'

const COURSE_TYPE_OPTIONS: { value: CourseType; label: string; description: string }[] = [
  { value: 'RUNNING_COURSE', label: '러닝 코스', description: '직접 달릴 경로로 저장' },
  { value: 'SPOT_COURSE', label: '스팟 코스', description: '방문 장소 중심으로 저장' },
]

function toggleId(ids: string[], id: string) {
  return ids.includes(id) ? ids.filter((item) => item !== id) : [...ids, id]
}

function isApiMessageError(error: unknown): error is { response?: { data?: { message?: string } } } {
  return typeof error === 'object' && error !== null && 'response' in error
}

export function CourseSaveDetailPage() {
  const navigate = useNavigate()
  const waypoints = useCourseDraftStore((state) => state.waypoints)
  const draftRoute = useCourseDraftStore((state) => state.draftRoute)
  const resetDraft = useCourseDraftStore((state) => state.resetDraft)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [courseType, setCourseType] = useState<CourseType>('RUNNING_COURSE')
  const [selectedThemeIds, setSelectedThemeIds] = useState<string[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([])
  const [isPublic, setIsPublic] = useState(true)
  const [themes, setThemes] = useState<ThemeOption[]>([])
  const [courseTags, setCourseTags] = useState<CourseTagOption[]>([])
  const [loadStatus, setLoadStatus] = useState<LoadStatus>('idle')
  const [submitStatus, setSubmitStatus] = useState<SubmitStatus>('idle')
  const [errorMessage, setErrorMessage] = useState('')
  const [createdCourseId, setCreatedCourseId] = useState<string | null>(null)

  const suggestedName = useMemo(() => {
    if (waypoints.length < 2) return ''
    const firstName = waypoints[0].name
    const lastName = waypoints[waypoints.length - 1].name
    return `${firstName} - ${lastName} 러닝 코스`
  }, [waypoints])

  useEffect(() => {
    if (name || !suggestedName) return
    setName(suggestedName.slice(0, 80))
  }, [name, suggestedName])

  useEffect(() => {
    if (waypoints.length < 2 || !draftRoute) {
      navigate('/courses/create', { replace: true })
    }
  }, [draftRoute, navigate, waypoints.length])

  useEffect(() => {
    let disposed = false
    setLoadStatus('loading')
    Promise.all([
      courseBuilderService.getThemes(),
      courseBuilderService.getCourseTags(),
    ])
      .then(([themeOptions, tagOptions]) => {
        if (disposed) return
        setThemes(themeOptions)
        setCourseTags(tagOptions)
        setLoadStatus('success')
      })
      .catch(() => {
        if (disposed) return
        setThemes([])
        setCourseTags([])
        setLoadStatus('error')
      })

    return () => {
      disposed = true
    }
  }, [])

  const canSubmit = name.trim().length > 0
    && waypoints.length >= 2
    && draftRoute !== null
    && submitStatus !== 'saving'

  async function handleSubmit() {
    if (!canSubmit) return
    setSubmitStatus('saving')
    setErrorMessage('')

    try {
      const response = await courseBuilderService.createCourse({
        name: name.trim(),
        description: description.trim() || null,
        courseType,
        waypoints,
        themeIds: selectedThemeIds,
        tagIds: selectedTagIds,
        isPublic,
      })
      setCreatedCourseId(response.courseId)
      setSubmitStatus('success')
    } catch (error: unknown) {
      const message = isApiMessageError(error)
        ? error.response?.data?.message
        : undefined
      setErrorMessage(message || '코스를 저장하지 못했어요. 경유지와 네트워크 상태를 확인해 주세요.')
      setSubmitStatus('error')
    }
  }

  function finishAndGoCourses() {
    resetDraft()
    navigate('/running/courses', { replace: true })
  }

  function finishAndCreateAnother() {
    resetDraft()
    navigate('/courses/create', { replace: true })
  }

  function finishAndStartRun() {
    if (!createdCourseId) return
    const courseName = name.trim() || suggestedName || '새 코스'
    resetDraft()
    navigate('/running/free', {
      replace: true,
      state: {
        courseId: createdCourseId,
        courseName,
        runningMode: 'COURSE_CREATE',
      },
    })
  }

  return (
    <main className="course-save-page">
      <header className="course-save-header">
        <IconButton
          icon={<Icon name="arrowLeft" />}
          label="뒤로 가기"
          onClick={() => navigate('/courses/create')}
        />
        <strong>코스 저장</strong>
        <span />
      </header>

      <section className="course-save-summary">
        <div>
          <span>총 거리</span>
          <strong>{draftRoute ? formatDistanceKm(draftRoute.distanceKm) : '0'}<small>km</small></strong>
        </div>
        <div>
          <span>예상 시간</span>
          <strong>{draftRoute?.estimatedDurationMinutes ?? 0}<small>분</small></strong>
        </div>
        <div>
          <span>난이도</span>
          <strong>{draftRoute ? difficultyLabel(draftRoute.suggestedDifficulty) : '-'}</strong>
        </div>
      </section>

      <section className="course-save-section">
        <SectionHeader title="기본 정보" className="course-save-section-heading" />
        <Input label="코스 이름" value={name} maxLength={80} onChange={(event) => setName(event.target.value)} placeholder="코스 이름" count={`${name.length}/80`} />
        <Textarea label="소개" value={description} maxLength={500} onChange={(event) => setDescription(event.target.value)} placeholder="이 코스의 분위기나 주의할 점을 적어주세요." count={`${description.length}/500`} />
      </section>

      <section className="course-save-section">
        <SectionHeader title="코스 유형" className="course-save-section-heading" />
        <div className="course-type-options">
          {COURSE_TYPE_OPTIONS.map((option) => (
            <Chip
              key={option.value}
              variant="choice"
              selected={courseType === option.value}
              onClick={() => setCourseType(option.value)}
            >
              <strong>{option.label}</strong>
              <small>{option.description}</small>
            </Chip>
          ))}
        </div>
      </section>

      <OptionSection
        title="테마"
        emptyText="선택 가능한 테마가 아직 없어요."
        options={themes.map((theme) => ({ id: theme.id, label: theme.name }))}
        selectedIds={selectedThemeIds}
        onToggle={(id) => setSelectedThemeIds((ids) => toggleId(ids, id))}
      />

      <OptionSection
        title="태그"
        emptyText="선택 가능한 태그가 아직 없어요."
        options={courseTags.map((tag) => ({ id: tag.id, label: tag.name }))}
        selectedIds={selectedTagIds}
        onToggle={(id) => setSelectedTagIds((ids) => toggleId(ids, id))}
      />

      {loadStatus === 'error' && (
        <p className="course-save-notice">선택 목록을 불러오지 못했지만 코스 저장은 가능해요.</p>
      )}

      <section className="course-save-section">
        <SectionHeader title="공개 설정" className="course-save-section-heading" />
        <Switch
          checked={isPublic}
          label={isPublic ? '공개 코스' : '비공개 코스'}
          description={isPublic ? '다른 러너가 이 코스를 발견할 수 있어요.' : '나만 볼 수 있게 저장해요.'}
          onCheckedChange={setIsPublic}
        />
      </section>

      <section className="course-save-section">
        <SectionHeader title="경유지" className="course-save-section-heading" />
        <ol className="course-save-waypoints">
          {waypoints.map((waypoint, index) => (
            <li key={`${waypoint.kakaoPlaceId}-${waypoint.orderIndex}`}>
              <span>{index + 1}</span>
              <div>
                <strong>{waypoint.name}</strong>
                <small>{waypoint.address || waypoint.categoryName || '주소 정보 없음'}</small>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {errorMessage && <p className="course-save-error">{errorMessage}</p>}

      <div className="course-save-footer">
        <Button variant="primary" size="lg" fullWidth loading={submitStatus === 'saving'} disabled={!canSubmit} onClick={handleSubmit}>코스 저장하기</Button>
      </div>

      <BottomSheet
        open={submitStatus === 'success' && Boolean(createdCourseId)}
        title="코스를 저장했어요"
        description="바로 달리거나 다음 코스를 이어서 만들 수 있어요."
        closeLabel="코스 선택으로 이동"
        closeOnBackdrop={false}
        closeOnEscape={false}
        onClose={finishAndGoCourses}
        className="course-save-success-sheet"
        footer={(
          <div className="course-save-success-actions">
            <Button variant="primary" size="lg" fullWidth onClick={finishAndStartRun}>이 코스로 달리기</Button>
            <Button variant="secondary" size="md" fullWidth onClick={finishAndCreateAnother}>새 코스 만들기</Button>
            <Button variant="ghost" size="md" fullWidth onClick={finishAndGoCourses}>코스 선택으로</Button>
          </div>
        )}
      >
        <div className="course-save-success">
          <span><Icon name="check" size={28} /></span>
          {/*<p>코스 ID</p>*/}
          {/*<code>{createdCourseId}</code>*/}
        </div>
      </BottomSheet>
    </main>
  )
}

function OptionSection({
  title,
  emptyText,
  options,
  selectedIds,
  onToggle,
}: {
  title: string
  emptyText: string
  options: { id: string; label: string }[]
  selectedIds: string[]
  onToggle: (id: string) => void
}) {
  return (
    <section className="course-save-section">
      <SectionHeader title={title} className="course-save-section-heading" />
      {options.length === 0 ? (
        <p className="course-save-empty-options">{emptyText}</p>
      ) : (
        <div className="course-save-chips">
          {options.map((option) => (
            <Chip
              key={option.id}
              variant="choice"
              selected={selectedIds.includes(option.id)}
              onClick={() => onToggle(option.id)}
            >
              {option.label}
            </Chip>
          ))}
        </div>
      )}
    </section>
  )
}
