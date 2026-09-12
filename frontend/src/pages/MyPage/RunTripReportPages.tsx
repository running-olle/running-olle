import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Badge, Button, Card, EmptyState, Icon, Input, ListRow, Modal, SectionHeader, Spinner } from '../../components/ui'
import { myPageService } from '../../features/mypage/myPageService'
import type { RunRecord, RunTripOverallStatistics, RunTripReportDetail, RunTripReportStatistics, RunTripReportSummary, Visit } from '../../features/mypage/types'
import { MyPageHeader as PageHeader, MyPageLoading as Loading } from './MyPageCommon'
import './mypage.css'

function assetUrl(url: string) {
  if (!url.startsWith('/')) return url
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined
  return apiBaseUrl?.startsWith('http') ? `${new URL(apiBaseUrl).origin}${url}` : url
}

function imageStyle(url: string | null) {
  return url ? { backgroundImage: `url(${assetUrl(url)})` } : undefined
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
}

function formatPeriod(startDate: string, endDate: string) {
  return `${formatDate(startDate)} – ${formatDate(endDate)}`
}

function formatTime(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60
  return hours
    ? `${hours}시간 ${minutes}분`
    : `${minutes}분 ${String(remainingSeconds).padStart(2, '0')}초`
}

function formatPace(value: number | null) {
  if (!value) return '-'
  const totalSeconds = Math.round(value * 60)
  const minutes = Math.floor(totalSeconds / 60)
  return `${minutes}'${String(totalSeconds % 60).padStart(2, '0')}\"`
}

function LoadError({ retry }: { retry: () => void }) {
  return <EmptyState className="my-empty" icon={<Icon name="route" size={28}/>} title="리포트를 불러오지 못했어요" description="잠시 후 다시 시도해 주세요." action={<Button variant="secondary" size="sm" onClick={retry}>다시 불러오기</Button>}/>
}

function ReportList({ reports }: { reports: RunTripReportSummary[] }) {
  return <div className="report-list">
    {reports.map(report => <Link to={`/mypage/reports/${report.id}`} key={report.id} className="report-list-card">
      <ListRow
        className="report-list-row"
        leading={<div className="report-list-image" style={imageStyle(report.thumbnailImageUrl)}>{!report.thumbnailImageUrl && <Icon name="route" size={32}/>}</div>}
        title={report.name}
        description={formatPeriod(report.startDate, report.endDate)}
        trailing={<Icon name="chevronRight" size={20}/>}
      />
    </Link>)}
  </div>
}

function OverallStatistics({ statistics }: { statistics: RunTripOverallStatistics }) {
  return <>
    <p className="report-scope-note">생성한 런트립 리포트에 포함된 활동을 기준으로 집계했어요.</p>
    <section className="report-stat-grid report-stat-grid--overall" aria-label="전체 런트립 통계">
      <Stat label="총 런트립" value={String(statistics.reportCount)} unit="개"/>
      <Stat label="총 러닝" value={String(statistics.runCount)} unit="회"/>
      <Stat label="누적 거리" value={statistics.totalDistanceKm.toFixed(1)} unit="km"/>
      <Stat label="러닝 시간" value={formatTime(statistics.totalDurationSeconds)}/>
      <Stat label="달린 코스" value={String(statistics.uniqueCourseCount)} unit="개"/>
      <Stat label="방문 장소" value={String(statistics.uniqueVisitedPlaceCount)} unit="곳"/>
    </section>
    <Card as="section" shadow="none" padding="lg" className="report-card">
      <h3>나의 런트립 브리핑</h3>
      <p>전체 평균 페이스 <strong>{formatPace(statistics.averagePace)}/km</strong></p>
      <p>런트립당 평균 거리 <strong>{statistics.averageDistancePerReport.toFixed(1)}km</strong></p>
    </Card>
  </>
}

export function ReportsPage() {
  const [tab, setTab] = useState<'trip' | 'all'>('trip')
  const [reports, setReports] = useState<RunTripReportSummary[] | null>(null)
  const [statistics, setStatistics] = useState<RunTripOverallStatistics | null>(null)
  const [reportError, setReportError] = useState(false)
  const [statisticsError, setStatisticsError] = useState(false)

  const loadReports = () => {
    setReportError(false)
    setReports(null)
    myPageService.reports().then(setReports).catch(() => setReportError(true))
  }
  const loadStatistics = () => {
    setStatisticsError(false)
    setStatistics(null)
    myPageService.reportStatistics().then(setStatistics).catch(() => setStatisticsError(true))
  }

  useEffect(() => { loadReports(); loadStatistics() }, [])

  return <div className="my-screen report-bg">
    <PageHeader title="런트립 리포트" action={<Link className="header-action" to="/mypage/reports/new"><Icon name="plus" size={18}/>새 리포트</Link>}/>
    <div className="report-tabs" role="tablist" aria-label="런트립 리포트 보기">
      <button role="tab" aria-selected={tab === 'trip'} className={tab === 'trip' ? 'active' : ''} onClick={() => setTab('trip')}>여행별 리포트</button>
      <button role="tab" aria-selected={tab === 'all'} className={tab === 'all' ? 'active' : ''} onClick={() => setTab('all')}>전체 통계</button>
    </div>
    <main className="my-content report-content">
      {tab === 'trip'
        ? reportError
          ? <LoadError retry={loadReports}/>
          : reports === null
            ? <Loading/>
            : reports.length
              ? <ReportList reports={reports}/>
              : <EmptyState className="my-empty" icon={<Icon name="route" size={28}/>} title="아직 런트립 리포트가 없어요" description="여행 기간을 선택하면 러닝과 방문 기록을 하나의 리포트로 정리해드려요." action={<Link className="ui-button ui-button--primary ui-button--md" to="/mypage/reports/new">첫 리포트 만들기</Link>}/>
        : statisticsError
          ? <LoadError retry={loadStatistics}/>
          : statistics === null
            ? <Loading label="전체 통계를 불러오는 중…"/>
            : <OverallStatistics statistics={statistics}/>
      }
    </main>
  </div>
}

function Stat({ label, value, unit }: { label: string; value: string; unit?: string }) {
  return <Card variant="stat" shadow="none"><span>{label}</span><b>{value}{unit && <small>{unit}</small>}</b></Card>
}

function ReportRunCard({ run }: { run: RunRecord }) {
  const type = run.courseType === 'SPOT_COURSE' ? '스팟 코스' : run.courseType ? '러닝 코스' : '자유 러닝'
  return <Link className="report-run-card" to={`/mypage/history/${run.id}`}>
    <Card variant="interactive" shadow="none" className="report-run-card__surface">
      <div className="report-run-card__top"><Badge variant={run.courseType === 'SPOT_COURSE' ? 'spot' : 'neutral'}>{type}</Badge><time>{new Date(run.startedAt).toLocaleDateString('ko-KR')}</time></div>
      <h3>{run.courseName || '나만의 자유 러닝'}</h3>
      <p><strong>{run.distanceKm.toFixed(1)}km</strong><span>{formatTime(run.durationSeconds)}</span><span>{formatPace(run.averagePace)}/km</span></p>
    </Card>
  </Link>
}

function ReportVisitCard({ visit }: { visit: Visit }) {
  const mapUrl = `https://map.kakao.com/link/map/${encodeURIComponent(visit.name)},${visit.latitude},${visit.longitude}`
  return <Card as="article" shadow="none" padding="sm" className="report-visit-card">
    <div className="report-visit-image" style={imageStyle(visit.imageUrl)}>{!visit.imageUrl && <Icon name="location" size={26}/>}</div>
    <div><time>{new Date(visit.visitedAt).toLocaleDateString('ko-KR')}</time><h3>{visit.name}</h3><p>{visit.courseName}</p></div>
    <a href={mapUrl} target="_blank" rel="noreferrer" aria-label={`${visit.name} 지도에서 보기`}><Icon name="location" size={20}/></a>
  </Card>
}

function Breakdown({ detail }: { detail: RunTripReportDetail }) {
  const { runningCourseRuns, spotCourseRuns, freeRuns } = detail.breakdown
  const total = runningCourseRuns + spotCourseRuns + freeRuns
  const width = (value: number) => total ? `${value / total * 100}%` : '0%'
  return <Card as="section" shadow="none" padding="lg" className="report-card report-breakdown">
    <h3>러닝 구성</h3>
    {total ? <>
      <div className="report-breakdown-bar" aria-label={`러닝 코스 ${runningCourseRuns}회, 스팟 코스 ${spotCourseRuns}회, 자유 러닝 ${freeRuns}회`}>
        <i className="running" style={{ width: width(runningCourseRuns) }}/><i className="spot" style={{ width: width(spotCourseRuns) }}/><i className="free" style={{ width: width(freeRuns) }}/>
      </div>
      <div className="report-breakdown-legend"><span className="running">러닝 코스 <b>{runningCourseRuns}</b></span><span className="spot">스팟 코스 <b>{spotCourseRuns}</b></span><span className="free">자유 러닝 <b>{freeRuns}</b></span></div>
    </> : <p>이 기간에는 아직 러닝 기록이 없어요.</p>}
  </Card>
}

export function RunTripReportDetailPage() {
  const { reportId } = useParams()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<RunTripReportDetail | null>(null)
  const [failed, setFailed] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [deleteError, setDeleteError] = useState('')

  const load = () => {
    if (!reportId) return setFailed(true)
    setFailed(false)
    setDetail(null)
    myPageService.report(reportId).then(setDetail).catch(() => setFailed(true))
  }
  useEffect(load, [reportId])

  const remove = async () => {
    if (!reportId) return
    setDeleting(true)
    setDeleteError('')
    try {
      await myPageService.deleteReport(reportId)
      navigate('/mypage/reports', { replace: true })
    } catch {
      setDeleting(false)
      setDeleteError('리포트를 삭제하지 못했어요. 잠시 후 다시 시도해 주세요.')
    }
  }

  if (failed) return <div className="my-screen"><PageHeader title="런트립 리포트"/><main className="my-content"><LoadError retry={load}/></main></div>
  if (!detail) return <div className="my-screen"><PageHeader title="런트립 리포트"/><main className="my-content"><Loading/></main></div>

  const stats = detail.statistics
  return <div className="my-screen">
    <PageHeader title="런트립 리포트" action={<Link className="header-action" to={`/mypage/reports/${detail.id}/edit`}><Icon name="edit" size={17}/>수정</Link>}/>
    <main className="my-content report-detail-content">
      <section className="trip-hero" style={imageStyle(detail.thumbnailImageUrl)}><span>RUNTRIP</span><h1>{detail.name}</h1><p>{formatPeriod(detail.startDate, detail.endDate)}</p></section>
      <section className="report-stat-grid" aria-label="여행별 러닝 통계">
        <Stat label="러닝 횟수" value={String(stats.runCount)} unit="회"/><Stat label="달린 거리" value={stats.totalDistanceKm.toFixed(1)} unit="km"/>
        <Stat label="러닝 시간" value={formatTime(stats.totalDurationSeconds)}/><Stat label="평균 페이스" value={formatPace(stats.averagePace)} unit="/km"/>
        <Stat label="달린 코스" value={String(stats.uniqueCourseCount)} unit="개"/><Stat label="방문 장소" value={String(stats.uniqueVisitedPlaceCount)} unit="곳"/>
      </section>
      <Breakdown detail={detail}/>
      <section className="report-detail-section"><SectionHeader title="러닝 기록"/><div className="report-record-list">{detail.runs.length ? detail.runs.map(run => <ReportRunCard run={run} key={run.id}/>) : <p className="report-inline-empty">이 기간에는 러닝 기록이 없어요.</p>}</div></section>
      <section className="report-detail-section"><SectionHeader title="방문 장소"/><div className="report-visit-list">{detail.visits.length ? detail.visits.map(visit => <ReportVisitCard visit={visit} key={visit.id}/>) : <p className="report-inline-empty">이 기간에는 방문 기록이 없어요.</p>}</div></section>
      <Button variant="danger" size="md" fullWidth onClick={() => { setDeleteError(''); setDeleteDialogOpen(true) }}>리포트 삭제</Button>
    </main>
    <Modal
      open={deleteDialogOpen}
      title="런트립 리포트를 삭제할까요?"
      description="리포트만 삭제되며 러닝과 방문 기록은 그대로 유지돼요."
      onClose={() => { if (!deleting) setDeleteDialogOpen(false) }}
      closeOnBackdrop={!deleting}
      closeOnEscape={!deleting}
      footer={<><Button variant="secondary" size="md" disabled={deleting} onClick={() => setDeleteDialogOpen(false)}>취소</Button><Button variant="danger" size="md" loading={deleting} onClick={remove}>삭제</Button></>}
    >
      {deleteError && <p className="my-error" role="alert">{deleteError}</p>}
    </Modal>
  </div>
}

function Preview({ statistics, loading }: { statistics: RunTripReportStatistics | null; loading: boolean }) {
  if (loading) return <div className="report-preview"><Spinner label="기간 내 기록 확인 중"/><span>기간 내 기록을 확인하고 있어요.</span></div>
  if (!statistics) return null
  return <section className="report-preview"><div><Icon name="sparkles" size={20}/><strong>이 기간의 기록</strong></div><p>러닝 <b>{statistics.runCount}회</b><i>·</i>거리 <b>{statistics.totalDistanceKm.toFixed(1)}km</b><i>·</i>방문 장소 <b>{statistics.uniqueVisitedPlaceCount}곳</b></p>{statistics.runCount === 0 && <small>빈 리포트로 만들어도 해당 기간에 기록이 추가되면 자동으로 반영돼요.</small>}</section>
}

export function RunTripReportFormPage() {
  const { reportId } = useParams()
  const editing = Boolean(reportId)
  const navigate = useNavigate()
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [name, setName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [thumbnailImageUrl, setThumbnailImageUrl] = useState<string | null>(null)
  const [preview, setPreview] = useState<RunTripReportStatistics | null>(null)
  const [loading, setLoading] = useState(editing)
  const [previewing, setPreviewing] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!reportId) return
    myPageService.report(reportId).then(report => {
      setName(report.name); setStartDate(report.startDate); setEndDate(report.endDate); setThumbnailImageUrl(report.thumbnailImageUrl); setLoading(false)
    }).catch(() => { setError('리포트 정보를 불러오지 못했어요.'); setLoading(false) })
  }, [reportId])

  useEffect(() => {
    if (!startDate || !endDate || endDate < startDate) { setPreview(null); return }
    let active = true
    setPreviewing(true)
    const timer = window.setTimeout(() => {
      myPageService.reportPreview(startDate, endDate)
        .then(result => { if (active) setPreview(result) })
        .catch(() => { if (active) setPreview(null) })
        .finally(() => { if (active) setPreviewing(false) })
    }, 300)
    return () => { active = false; window.clearTimeout(timer) }
  }, [startDate, endDate])

  const selectImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) return setError('이미지 파일만 선택할 수 있어요.')
    if (file.size > 10 * 1024 * 1024) return setError('대표 사진은 10MB 이하만 올릴 수 있어요.')
    setUploading(true); setError('')
    try {
      const url = await myPageService.uploadReportImage(file)
      if (!url) throw new Error('empty image url')
      setThumbnailImageUrl(url)
    } catch {
      setError('대표 사진을 업로드하지 못했어요.')
    } finally { setUploading(false) }
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!name.trim()) return setError('여행 이름을 입력해 주세요.')
    if (!startDate || !endDate) return setError('여행 기간을 입력해 주세요.')
    if (endDate < startDate) return setError('종료일은 시작일보다 빠를 수 없어요.')
    setSaving(true); setError('')
    try {
      const body = { name: name.trim(), startDate, endDate, thumbnailImageUrl }
      const report = reportId ? await myPageService.updateReport(reportId, body) : await myPageService.createReport(body)
      navigate(`/mypage/reports/${report.id}`, { replace: true })
    } catch {
      setError('리포트를 저장하지 못했어요. 입력 내용을 확인해 주세요.')
      setSaving(false)
    }
  }

  if (loading) return <div className="my-screen"><PageHeader title={editing ? '리포트 수정' : '새 리포트 만들기'}/><main className="my-content"><Loading/></main></div>

  return <div className="my-screen">
    <PageHeader title={editing ? '리포트 수정' : '새 리포트 만들기'}/>
    <form className="my-content report-form" onSubmit={submit}>
      <div className="form-intro"><span><Icon name="route" size={24}/></span><div><h2>여행의 러닝을 한곳에 모아요</h2><p>선택한 기간의 러닝과 방문 기록이 자동으로 반영돼요.</p></div></div>
      <Input required label="여행 이름" value={name} maxLength={200} placeholder="예) 2026 제주 여름 런트립" onChange={event => { setName(event.target.value); setError('') }}/>
      <div className="date-pair"><Input required type="date" label="시작일" value={startDate} onChange={event => { setStartDate(event.target.value); setError('') }}/><Input required type="date" label="종료일" min={startDate || undefined} value={endDate} onChange={event => { setEndDate(event.target.value); setError('') }}/></div>
      <div className="report-image-field"><span>대표 사진</span><input ref={fileInputRef} className="profile-file-input" type="file" accept="image/*" onChange={selectImage}/><button type="button" className={`report-image-picker ${thumbnailImageUrl ? 'has-image' : ''}`} style={imageStyle(thumbnailImageUrl)} onClick={() => fileInputRef.current?.click()} disabled={uploading}><Icon name="camera" size={28}/><b>{uploading ? '업로드 중…' : thumbnailImageUrl ? '대표 사진 변경' : '대표 사진 선택'}</b></button>{thumbnailImageUrl && <button className="report-image-remove" type="button" onClick={() => setThumbnailImageUrl(null)} disabled={uploading}>사진 삭제</button>}</div>
      <Preview statistics={preview} loading={previewing}/>
      {error && <p className="my-error" role="alert">{error}</p>}
      <Button type="submit" variant="primary" size="lg" fullWidth loading={saving} disabled={uploading}>{editing ? '리포트 저장하기' : '런트립 리포트 만들기'}</Button>
    </form>
  </div>
}
