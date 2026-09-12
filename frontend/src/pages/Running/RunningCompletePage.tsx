import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { FreeRunningMap } from '../../features/running/FreeRunningMap'
import { saveRunningRouteAsCourse } from '../../features/running/runningRecordService'
import { formatDistance, formatDuration, formatPace } from '../../features/running/runningUtils'
import type { SavedRunningRecord } from '../../features/running/types'
import { Button, Input } from '../../components/ui'

export function RunningCompletePage() {
  const navigate = useNavigate()
  const record = useLocation().state?.record as SavedRunningRecord | undefined
  const [showCourseSave, setShowCourseSave] = useState(false)
  const [courseName, setCourseName] = useState('')
  const [isPublic, setIsPublic] = useState(true)
  const [savingCourse, setSavingCourse] = useState(false)
  const [courseSaveError, setCourseSaveError] = useState('')
  const [savedCourseId, setSavedCourseId] = useState<string | null>(null)
  if (!record) return <Navigate to="/running" replace />

  const canSaveCourse = record.runningMode === 'FREE_RUN' && record.syncStatus === 'synced' && Boolean(record.serverId)

  async function handleCourseSave() {
    const name = courseName.trim()
    if (!record?.serverId || !name || savingCourse) return
    setSavingCourse(true)
    setCourseSaveError('')
    try {
      const result = await saveRunningRouteAsCourse(record.serverId, { name, isPublic })
      setSavedCourseId(result.courseId)
      setShowCourseSave(false)
    } catch (error: unknown) {
      const apiMessage = typeof error === 'object' && error !== null && 'response' in error
        ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
        : undefined
      setCourseSaveError(apiMessage || '코스를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setSavingCourse(false)
    }
  }

  return (
    <main className="running-complete-page">
      <section className="complete-copy"><span>✓</span><p>러닝 완료</p><h1>오늘도 멋지게 달렸어요!</h1></section>
      <div className="complete-map"><FreeRunningMap currentPosition={record.route.at(-1) ?? null} recordedPath={record.route} followPosition={false} /></div>
      <section className="complete-stats">
        <div><strong>{formatDistance(record.distanceMeters)}</strong><span>km</span></div>
        <div><strong>{formatDuration(record.durationSeconds)}</strong><span>시간</span></div>
        <div><strong>{formatPace(record.averagePace)}</strong><span>평균 페이스</span></div>
      </section>
      {record.syncStatus === 'pending' && <p className="record-sync-notice">서버 연결에 실패해 기록을 이 기기에 임시 저장했어요.</p>}
      {canSaveCourse && !savedCourseId && (
        <Button className="complete-course-save-button" variant="secondary" size="lg" fullWidth onClick={() => setShowCourseSave(true)}>달린 경로를 코스로 저장</Button>
      )}
      {savedCourseId && (
        <section className="complete-course-saved">
          <strong>‘{courseName.trim()}’ 코스로 저장했어요.</strong>
          <button type="button" onClick={() => navigate(`/courses/${savedCourseId}`)}>코스 보기</button>
        </section>
      )}
      <Button className="complete-home-button" variant="primary" size="lg" fullWidth onClick={() => navigate('/', { replace: true })}>홈으로</Button>
      {showCourseSave && (
        <div className="course-save-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="running-course-save-title" onClick={() => !savingCourse && setShowCourseSave(false)}>
          <section className="running-course-save-modal" onClick={(event) => event.stopPropagation()}>
            <h2 id="running-course-save-title">달린 경로를 코스로 저장</h2>
            <p>자유 러닝에서 기록한 경로가 그대로 새 코스가 돼요.</p>
            <Input label="코스 이름" autoFocus value={courseName} maxLength={200} placeholder="예: 노을 따라 해안 러닝" count={`${courseName.length}/200`} onChange={(event) => setCourseName(event.target.value)} />
            <button className="running-course-public-toggle" type="button" role="switch" aria-checked={isPublic} onClick={() => setIsPublic((value) => !value)}>
              <span><strong>{isPublic ? '공개 코스' : '비공개 코스'}</strong><small>{isPublic ? '다른 러너도 이 코스를 볼 수 있어요.' : '나만 볼 수 있게 저장해요.'}</small></span>
              <i className={isPublic ? 'on' : ''}><em /></i>
            </button>
            {courseSaveError && <p className="running-course-save-error">{courseSaveError}</p>}
            <div className="running-course-save-actions">
              <Button variant="secondary" disabled={savingCourse} onClick={() => setShowCourseSave(false)}>취소</Button>
              <Button variant="primary" loading={savingCourse} disabled={!courseName.trim()} onClick={handleCourseSave}>코스 저장</Button>
            </div>
          </section>
        </div>
      )}
    </main>
  )
}
