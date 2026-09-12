import { useEffect, useMemo, useRef, useState, type ChangeEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { axiosInstance } from '../../api/axiosInstance'
import { AppHeader } from '../../components/layout/Header'
import { Badge, Button, Chip, EmptyState, Icon, Input, SectionHeader, Switch, Textarea, Toast, type IconName } from '../../components/ui'
import { CourseListView } from '../../features/course/CourseListView'
import { courseService } from '../../features/course/courseService'
import { myPageService } from '../../features/mypage/myPageService'
import { NotificationCenter } from '../../features/notifications/NotificationCenter'
import type { Dashboard, NotificationSettings, Profile, RunRecord, Visit } from '../../features/mypage/types'
import { MyPageHeader as PageHeader, MyPageLoading as Loading } from './MyPageCommon'
import './mypage.css'

const EMPTY_PROFILE: Profile = { nickname: '러너', profileImageUrl: null, bio: null, userTypes: [], preferredDistance: null, preferredDifficulty: null, createdAt: '', accountStatus: 'ACTIVE' }
const EMPTY_DASHBOARD: Dashboard = { profile: EMPTY_PROFILE, totalDistanceKm: 0, completionCount: 0, uniqueCourseCount: 0 }
const DEFAULT_NOTIFICATIONS: NotificationSettings = { recommendedCourse: true, weather: true, savedCourseUpdate: true, meetupInvite: true, commentLike: true, tierChange: true, eventChallenge: true }
function assetUrl(url: string) {
  if (!url.startsWith('/')) return url
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined
  return apiBaseUrl?.startsWith('http') ? `${new URL(apiBaseUrl).origin}${url}` : url
}
function Avatar({ profile, large = false }: { profile: Profile; large?: boolean }) { return <div className={`my-avatar ${large ? 'large' : ''}`} style={profile.profileImageUrl ? { backgroundImage: `url(${assetUrl(profile.profileImageUrl)})` } : undefined}>{!profile.profileImageUrl && profile.nickname.slice(0, 1)}</div> }
function Empty({ icon = 'history', title, description, action }: { icon?: IconName; title: string; description: string; action?: ReactNode }) { return <EmptyState className="my-empty" icon={<Icon name={icon} size={28}/>} title={title} description={description} action={action}/> }
function formatTime(seconds: number) { const h = Math.floor(seconds / 3600), m = Math.floor(seconds % 3600 / 60), s = seconds % 60; return h ? `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}` : `${m}:${String(s).padStart(2,'0')}` }
function pace(value: number | null) { if (!value) return '-'; const min = Math.floor(value); return `${min}'${String(Math.round((value-min)*60)).padStart(2,'0')}\"` }
function imageStyle(url: string | null) { return url ? { backgroundImage: `url(${url})` } : undefined }

export function MyPage() {
  const [data, setData] = useState<Dashboard | null>(null); const [failed, setFailed] = useState(false)
  useEffect(() => { myPageService.dashboard().then(setData).catch(() => { setFailed(true); setData(EMPTY_DASHBOARD) }) }, [])
  if (!data) return <div className="my-screen"><AppHeader variant="root" leading="마이페이지"/><main className="my-content"><Loading label="마이페이지를 불러오는 중…"/></main></div>
  const { profile } = data
  return <div className="my-screen my-home"><AppHeader variant="root" leading="마이페이지" trailing={<NotificationCenter />}/><main className="my-content my-home-content">{failed && <p className="my-error" role="alert">데이터를 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>}
    <section className="my-profile"><Link to="/mypage/settings/profile" className="avatar-edit" aria-label="프로필 편집"><Avatar profile={profile} large/><span><Icon name="edit" size={14}/></span></Link><h2>{profile.nickname}</h2><p>{profile.bio || '오늘도 나답게, 즐겁게 달려요.'}</p><Badge variant="brand"><Icon name="star" size={14}/>{typeLabels[profile.userTypes[0]] || '러닝 입문자'}</Badge></section>
    <section className="my-stats" aria-label="누적 러닝 통계"><div><span>누적 거리</span><strong>{data.totalDistanceKm.toFixed(1)}<small>km</small></strong></div><div><span>총 완주</span><strong>{data.completionCount}<small>회</small></strong></div><div><span>달린 코스</span><strong>{data.uniqueCourseCount}<small>개</small></strong></div></section>
    <nav className="my-menu" aria-label="마이페이지 메뉴"><Menu to="/mypage/history" icon="history" label="러닝 히스토리" description="완주 기록과 방문 장소"/><Menu to="/mypage/bookmarks" icon="bookmark" label="저장한 코스" description="내 코스와 북마크"/><Menu to="/mypage/reports" icon="chart" label="런트립 리포트" description="여행별 러닝 통계"/><Menu to="/mypage/settings" icon="settings" label="설정" description="계정, 프로필, 알림"/></nav>
  </main></div>
}
function Menu({ to, icon, label, description }: { to: string; icon: IconName; label: string; description?: string }) { return <Link to={to}><span><Icon name={icon}/></span><span className="my-menu-copy"><b>{label}</b>{description && <small>{description}</small>}</span><Icon name="chevronRight" size={19}/></Link> }
function RunCard({ run }: { run: RunRecord }) { return <Link className="run-row" to={`/mypage/history/${run.id}`}><div className="thumb" style={imageStyle(run.thumbnailImageUrl)}>{!run.thumbnailImageUrl && <Icon name="run" size={28}/>}</div><div><div className="run-row-meta"><Badge variant={run.courseType === 'SPOT_COURSE' ? 'spot' : 'neutral'}>{run.courseType === 'SPOT_COURSE' ? '스팟 코스' : run.courseType ? '러닝 코스' : '자유 러닝'}</Badge><small>{new Date(run.startedAt).toLocaleDateString('ko-KR')}</small></div><h3>{run.courseName || '나만의 자유 러닝'}</h3><p><b>{run.distanceKm.toFixed(1)}km</b><i>·</i>{formatTime(run.durationSeconds)}<i>·</i>{pace(run.averagePace)}/km</p></div><Icon name="chevronRight" size={18}/></Link> }

function VisitCard({ visit, compact = false }: { visit: Visit; compact?: boolean }) {
  const mapUrl = `https://map.kakao.com/link/map/${encodeURIComponent(visit.name)},${visit.latitude},${visit.longitude}`
  return <article className={`visit-card ${compact ? 'compact' : ''}`}>
    <div className="visit-image" style={imageStyle(visit.imageUrl)}>{!visit.imageUrl && <Icon name="location" size={28}/>}</div>
    <div className="visit-copy">
      <div><span>{new Date(visit.visitedAt).toLocaleDateString('ko-KR')}</span><small>{visit.orderIndex}번째 지점</small></div>
      <h3>{visit.name}</h3>
      <p className="visit-course">{visit.courseName}</p>
      {!compact && visit.description && <p className="visit-description">{visit.description}</p>}
    </div>
    <a href={mapUrl} target="_blank" rel="noreferrer" aria-label={`${visit.name} 지도에서 보기`}><Icon name="location" size={20}/></a>
  </article>
}

export function RunningHistoryPage() {
  const [runs,setRuns]=useState<RunRecord[]|null>(null)
  const [visits,setVisits]=useState<Visit[]|null>(null)
  useEffect(()=>{
    myPageService.runs().then(setRuns).catch(()=>setRuns([]))
    myPageService.visits().then(setVisits).catch(()=>setVisits([]))
  },[])
  return <div className="my-screen"><PageHeader title="러닝 히스토리"/><main className="my-content">
    <SectionHeader className="section-head" title="최근 완주" action={<Link to="/mypage/history/all">전체보기</Link>}/>
    {runs===null
      ? <Loading/>
      : runs.length
        ? <div className="run-list">{runs.slice(0,3).map(r=><RunCard key={r.id} run={r}/>)}</div>
        : <Empty title="아직 러닝 기록이 없어요" description="첫 코스를 달리면 거리와 페이스가 여기에 기록돼요." action={<Link className="ui-button ui-button--primary ui-button--md" to="/running">러닝 시작하기</Link>}/>
    }
    <SectionHeader className="section-head visit-head" title="방문 장소" action={Boolean(visits?.length) ? <Link to="/mypage/history/visits">전체보기</Link> : undefined}/>
    {visits===null?<Loading/>:visits.length?<div className="visit-list">{visits.slice(0,2).map(v=><VisitCard compact key={v.id} visit={v}/>)}</div>:<Empty icon="location" title="기록된 방문 장소가 없어요" description="스팟 코스를 달리고 웨이포인트를 방문해보세요."/>}
  </main></div>
}

export function VisitedPlacesPage() {
  const [visits,setVisits]=useState<Visit[]|null>(null)
  useEffect(()=>{myPageService.visits().then(setVisits).catch(()=>setVisits([]))},[])
  return <div className="my-screen"><PageHeader title="방문 장소"/><main className="my-content">
    <div className="info-box">러닝 중 실제로 통과한 코스 지점을 최근 방문순으로 보여드려요.</div>
    {visits===null?<Loading/>:visits.length?<div className="visit-list visit-list-all">{visits.map(v=><VisitCard key={v.id} visit={v}/>)}</div>:<Empty icon="location" title="기록된 방문 장소가 없어요" description="코스의 웨이포인트를 방문하면 이곳에 기록돼요."/>}
  </main></div>
}
export function CompletedRunsPage() { const [runs,setRuns]=useState<RunRecord[]|null>(null);const [filter,setFilter]=useState('ALL');useEffect(()=>{myPageService.runs().then(setRuns).catch(()=>setRuns([]))},[]);const filtered=useMemo(()=>runs?.filter(r=>filter==='ALL'||r.courseType===filter)??[],[runs,filter]);const distance=runs?.reduce((s,r)=>s+r.distanceKm,0)??0;return <div className="my-screen"><PageHeader title="완주 코스"/><main className="my-content"><section className="summary-cards"><div><span>총 완주</span><b>{runs?.length??0}회</b></div><div><span>달린 코스</span><b>{new Set(runs?.map(r=>r.courseId).filter(Boolean)).size}개</b></div><div><span>누적 거리</span><b>{distance.toFixed(1)}km</b></div></section><Filter value={filter} setValue={setFilter} options={[['ALL','전체'],['RUNNING_COURSE','러닝 코스'],['SPOT_COURSE','스팟 코스']]}/>{runs===null?<Loading/>:filtered.length?<div className="run-list">{filtered.map(r=><RunCard key={r.id} run={r}/>)}</div>:<Empty title="조건에 맞는 완주 기록이 없어요" description="필터를 바꾸거나 새로운 코스에 도전해보세요."/>}</main></div> }
function Filter({value,setValue,options}:{value:string;setValue:(x:string)=>void;options:string[][]}){return <div className="chips scroll">{options.map(([v,l])=><Chip selected={value===v} onClick={()=>setValue(v)} key={v}>{l}</Chip>)}</div>}

export function BookmarksPage() { return <div className="my-screen"><PageHeader title="저장한 코스"/><main className="my-content"><CourseListView scope="LIBRARY" title="저장한 코스" subtitle="내가 만든 코스와 북마크한 코스를 모아볼 수 있어요." emptyTitle="저장한 코스가 없어요" emptyDescription="코스를 만들거나 마음에 드는 코스를 저장하면 여기서 바로 확인할 수 있어요." showHeader={false} showSearch showBookmarkedFilter onRemoveBookmark={(bookmarkId) => myPageService.removeBookmark(bookmarkId).then(() => undefined)} onDeleteCourse={(courseId) => courseService.deleteCourse(courseId).then(() => undefined)}/></main></div> }

export function SettingsPage(){const nav=useNavigate();const logout=()=>{localStorage.removeItem('runningOlleAccessToken');nav('/login',{replace:true})};const withdraw=async()=>{if(!confirm('정말 회원 탈퇴할까요? 러닝 기록은 복구할 수 없을 수 있어요.'))return;await axiosInstance.delete('/users/me');logout()};return <div className="my-screen settings-bg"><PageHeader title="설정"/><main className="my-content settings-content"><SettingGroup title="내 정보"><Menu to="/mypage/settings/account" icon="user" label="카카오 계정 정보" description="연결 계정과 가입 정보"/><Menu to="/mypage/settings/profile" icon="edit" label="프로필 편집" description="사진과 러닝 취향"/><Menu to="/mypage/settings/notifications" icon="bell" label="알림 설정" description="커뮤니티 소식 수신"/></SettingGroup><SettingGroup title="계정 관리"><button className="setting-button" onClick={logout}><span><Icon name="logout"/></span><span className="my-menu-copy"><b>로그아웃</b><small>이 기기에서 계정 연결 해제</small></span></button><button className="setting-button danger" onClick={withdraw}><span><Icon name="user"/></span><span className="my-menu-copy"><b>회원 탈퇴</b><small>계정과 러닝 기록 삭제</small></span></button></SettingGroup><footer><strong>Running Olle</strong><small>카카오 계정으로 안전하게 연결됨</small></footer></main></div>}
function SettingGroup({title,children}:{title:string;children:ReactNode}){return <section className="setting-group"><h2>{title}</h2><div>{children}</div></section>}
export function AccountPage(){const [p,setP]=useState<Profile|null>(null);useEffect(()=>{myPageService.profile().then(setP).catch(()=>setP(EMPTY_PROFILE))},[]);return <div className="my-screen settings-bg"><PageHeader title="계정 정보"/><main className="my-content account-content">{!p?<Loading/>:<><section className="account-panel"><div className="account-person"><Avatar profile={p}/><div><h2>{p.nickname}</h2><p>카카오 소셜 계정</p></div></div><dl><div><dt>로그인 방식</dt><dd><i className="kakao-dot">K</i> 카카오</dd></div><div><dt>가입일</dt><dd>{p.createdAt?new Date(p.createdAt).toLocaleDateString('ko-KR'):'-'}</dd></div><div><dt>계정 상태</dt><dd><Badge variant="success">정상</Badge></dd></div></dl></section><div className="info-box"><Icon name="sparkles" size={20}/><span>비밀번호와 카카오 계정 정보는 카카오에서 안전하게 관리돼요.</span></div><a className="ui-button ui-button--secondary ui-button--md my-full-action" href="https://accounts.kakao.com" target="_blank" rel="noreferrer">카카오 계정 관리 열기</a></>}</main></div>}

const typeLabels:Record<string,string>={ACTIVE_RUNNER:'활동적인 러너',RELAXED_TRAVELER:'여유로운 여행자',JEJU_RESIDENT:'제주 거주민'}
export function ProfileEditPage() {
  const nav = useNavigate()
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [p, setP] = useState<Profile | null>(null)
  const [loadError, setLoadError] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)

  const load = () => {
    setLoadError(false)
    myPageService.profile().then(setP).catch(() => setLoadError(true))
  }
  useEffect(load, [])

  if (loadError) return <div className="my-screen settings-bg"><PageHeader title="프로필 편집"/><main className="my-content"><EmptyState className="my-empty" title="프로필을 불러오지 못했어요" description="잠시 후 다시 시도해 주세요." action={<Button variant="secondary" size="sm" onClick={load}>다시 불러오기</Button>}/></main></div>
  if (!p) return <div className="my-screen"><Loading/></div>

  const selectImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) return setError('이미지 파일만 선택할 수 있어요.')
    if (file.size > 10 * 1024 * 1024) return setError('프로필 사진은 10MB 이하만 올릴 수 있어요.')
    setUploading(true)
    setError('')
    try {
      const imageUrl = await myPageService.uploadProfileImage(file)
      if (!imageUrl) throw new Error('empty image url')
      setP(current => current ? { ...current, profileImageUrl: imageUrl } : current)
    } catch {
      setError('프로필 사진을 업로드하지 못했어요.')
    } finally {
      setUploading(false)
    }
  }

  const save = async () => {
    const nickname = p.nickname.trim()
    if (nickname.length < 2) return setError('닉네임을 2자 이상 입력해 주세요.')
    setSaving(true)
    setError('')
    try {
      await myPageService.updateProfile({
        nickname,
        profileImageUrl: p.profileImageUrl,
        bio: p.bio?.trim() || null,
        userTypes: p.userTypes,
        preferredDistance: p.preferredDistance,
        preferredDifficulty: p.preferredDifficulty,
      })
      nav('/mypage')
    } catch (caught) {
      const response = (caught as { response?: { status?: number; data?: { detail?: string; message?: string } } }).response
      setError(response?.status === 409 ? '이미 사용 중인 닉네임이에요.' : response?.data?.detail || response?.data?.message || '프로필을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setSaving(false)
    }
  }

  return <div className="my-screen settings-bg"><PageHeader title="프로필 편집" action={<Button className="save-text" variant="ghost" size="sm" onClick={save} disabled={saving || uploading}>{saving ? '저장 중' : '저장'}</Button>}/><main className="my-content profile-form">
    <input ref={fileInputRef} className="profile-file-input" type="file" accept="image/*" onChange={selectImage}/>
    <button className="profile-avatar" type="button" onClick={() => fileInputRef.current?.click()} disabled={uploading} aria-label="프로필 사진 변경"><Avatar profile={p} large/><span><Icon name="edit" size={15}/></span></button>
    <div className="profile-image-actions"><button type="button" onClick={() => fileInputRef.current?.click()} disabled={uploading}>{uploading ? '업로드 중…' : '사진 변경'}</button>{p.profileImageUrl && <button type="button" className="danger" onClick={() => setP({...p, profileImageUrl: null})} disabled={uploading}>사진 삭제</button>}</div>
    <Input label="닉네임" value={p.nickname} minLength={2} maxLength={100} count={`${p.nickname.length} / 100자`} onChange={e => { setError(''); setP({...p,nickname:e.target.value}) }}/>
    <Textarea label="자기소개" value={p.bio || ''} maxLength={300} count={`${p.bio?.length || 0} / 300자`} onChange={e => { setError(''); setP({...p,bio:e.target.value}) }}/>
    <Choice title="사용자 유형" options={['ACTIVE_RUNNER','RELAXED_TRAVELER','JEJU_RESIDENT']} value={p.userTypes[0] || ''} labels={typeLabels} onChange={v => setP({...p,userTypes:[v]})}/>
    <Choice title="선호 거리" options={['UNDER_3KM','FROM_5_TO_10KM','OVER_10KM']} value={p.preferredDistance || ''} labels={{UNDER_3KM:'3km 이하',FROM_5_TO_10KM:'5km ~ 10km',OVER_10KM:'10km 이상'}} onChange={v => setP({...p,preferredDistance:v})}/>
    <Choice title="선호 난이도" options={['EASY','NORMAL','HARD']} value={p.preferredDifficulty || ''} labels={{EASY:'쉬움',NORMAL:'보통',HARD:'어려움'}} onChange={v => setP({...p,preferredDifficulty:v})}/>
    {error && <p className="my-error" role="alert">{error}</p>}
    <Button variant="primary" size="lg" fullWidth loading={saving} onClick={save} disabled={uploading}>저장하기</Button>
  </main></div>
}
function Choice({title,options,value,labels,onChange}:{title:string;options:string[];value:string;labels:Record<string,string>;onChange:(v:string)=>void}){return <section className="profile-choice"><h2>{title}</h2><div className="chips">{options.map(x=><Chip variant="choice" selected={value===x} onClick={()=>onChange(x)} key={x}>{labels[x]}</Chip>)}</div></section>}
export function NotificationPage(){const [settings,setSettings]=useState<NotificationSettings|null>(null);const [saved,setSaved]=useState(false);useEffect(()=>{myPageService.notifications().then(setSettings).catch(()=>setSettings(DEFAULT_NOTIFICATIONS))},[]);const toggle=async(key:keyof NotificationSettings)=>{if(!settings)return;const next={...settings,[key]:!settings[key]};setSettings(next);setSaved(false);try{await myPageService.updateNotifications(next);setSaved(true);setTimeout(()=>setSaved(false),1500)}catch{setSettings(settings)}};if(!settings)return <div className="my-screen"><Loading label="알림 설정을 불러오는 중…"/></div>;return <div className="my-screen settings-bg"><PageHeader title="알림 설정"/><main className="my-content notification-content"><div className="info-box"><Icon name="bell" size={20}/><span>알림은 앱 우측 상단의 알림함에 쌓입니다.</span></div><ToggleGroup title="커뮤니티" rows={[['meetupInvite','같이 달리기','참여 요청·승인·일정 변경·취소 알림'],['commentLike','댓글/좋아요','내 게시글에 대한 댓글과 좋아요 알림']]} settings={settings} toggle={toggle}/></main><Toast open={saved} message="알림 설정을 저장했어요" tone="success" onClose={()=>setSaved(false)} duration={1500}/></div>}
function ToggleGroup({title,rows,settings,toggle}:{title:string;rows:[keyof NotificationSettings,string,string][];settings:NotificationSettings;toggle:(k:keyof NotificationSettings)=>void}){return <section className="toggle-group"><h2>{title}</h2><div>{rows.map(([key,label,desc])=><Switch key={key} checked={settings[key]} label={label} description={desc} onCheckedChange={()=>toggle(key)}/>)}</div></section>}
