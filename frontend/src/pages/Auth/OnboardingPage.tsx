import { type ChangeEvent, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { axiosInstance } from '../../api/axiosInstance'
import type { ThemeOption } from '../../features/mypage/types'

type UserType = 'ACTIVE_RUNNER' | 'RELAXED_TRAVELER' | 'JEJU_RESIDENT'
type Distance = 'UNDER_3KM' | 'FROM_5_TO_10KM' | 'OVER_10KM'
type Difficulty = 'EASY' | 'NORMAL' | 'HARD'

const themeLabels: Record<string, string> = {
  COAST: '해안',
  FOREST: '숲길·곶자왈',
  OREUM: '오름',
  FOOD: '맛집',
  PHOTO: '포토 스팟',
  TRADITION: '전통 마을',
  URBAN: '도심',
}

const initialForm = {
  nickname: '',
  profileImageUrl: '',
  bio: '',
  userTypes: [] as UserType[],
  preferredDistance: '' as Distance | '',
  preferredDifficulty: '' as Difficulty | '',
  themeIds: [] as string[],
  terms: { service: false, privacy: false, location: false, marketing: false },
  notifications: { recommendedCourse: true, weather: true, meetupInvite: true, commentLike: false },
}

function Progress({ step }: { step: number }) {
  return (
    <div className="progress-wrap">
      <span>{step} / 3 단계</span>
      <div className="progress-bars">
        {[1, 2, 3].map((item) => <i key={item} className={item <= step ? 'active' : ''} />)}
      </div>
    </div>
  )
}

function Choice<T extends string>({ value, selected, label, emoji, onClick }: {
  value: T
  selected: boolean
  label: string
  emoji?: string
  onClick: (value: T) => void
}) {
  return <button type="button" className={`choice ${selected ? 'selected' : ''}`} onClick={() => onClick(value)}>{emoji} {label}</button>
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return <button type="button" role="switch" aria-checked={checked} className={`toggle ${checked ? 'on' : ''}`} onClick={onChange}><i /></button>
}

export function OnboardingPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [form, setForm] = useState(initialForm)
  const [themes, setThemes] = useState<ThemeOption[]>([])
  const [nicknameStatus, setNicknameStatus] = useState<'idle' | 'checking' | 'available' | 'taken'>('idle')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    axiosInstance.get<ThemeOption[]>('/themes')
      .then(({ data }) => setThemes(data))
      .catch(() => setThemes([]))
  }, [])

  useEffect(() => {
    if (form.nickname.trim().length < 2) {
      setNicknameStatus('idle')
      return
    }

    setNicknameStatus('checking')
    const timer = window.setTimeout(async () => {
      try {
        const { data } = await axiosInstance.get('/users/nickname-availability', { params: { nickname: form.nickname.trim() } })
        setNicknameStatus(data.available ? 'available' : 'taken')
      } catch {
        setNicknameStatus('idle')
      }
    }, 350)

    return () => window.clearTimeout(timer)
  }, [form.nickname])

  const stepValid = useMemo(() => {
    if (step === 1) return form.nickname.trim().length >= 2 && nicknameStatus !== 'taken'
    if (step === 2) return form.userTypes.length > 0 && !!form.preferredDistance && !!form.preferredDifficulty
    return form.terms.service && form.terms.privacy && form.terms.location
  }, [form, nicknameStatus, step])

  const toggleUserType = (value: UserType) => {
    setForm((prev) => ({
      ...prev,
      userTypes: prev.userTypes.includes(value)
        ? prev.userTypes.filter((item) => item !== value)
        : [...prev.userTypes, value],
    }))
  }

  const toggleTheme = (themeId: string) => {
    setForm((prev) => ({
      ...prev,
      themeIds: prev.themeIds.includes(themeId)
        ? prev.themeIds.filter((id) => id !== themeId)
        : [...prev.themeIds, themeId],
    }))
  }

  const selectPhoto = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    if (file.size > 3 * 1024 * 1024) {
      setError('프로필 사진은 3MB 이하만 선택할 수 있습니다.')
      return
    }

    const reader = new FileReader()
    reader.onload = () => setForm((prev) => ({ ...prev, profileImageUrl: String(reader.result) }))
    reader.readAsDataURL(file)
  }

  const goBack = () => step === 1 ? navigate('/login') : setStep((value) => value - 1)

  const next = async () => {
    setError('')
    if (!stepValid) return
    if (step < 3) {
      setStep((value) => value + 1)
      window.scrollTo(0, 0)
      return
    }

    setSubmitting(true)
    try {
      await axiosInstance.post('/users/me/onboarding', form)
      navigate('/', { replace: true })
    } catch (requestError: unknown) {
      const message = isApiError(requestError)
        ? requestError.response?.data?.message
        : undefined
      setError(message || '가입 정보를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  const allTerms = Object.values(form.terms).every(Boolean)
  const setTerm = (key: keyof typeof form.terms, value: boolean) => {
    setForm((prev) => ({ ...prev, terms: { ...prev.terms, [key]: value } }))
  }
  const setNotification = (key: keyof typeof form.notifications) => {
    setForm((prev) => ({ ...prev, notifications: { ...prev.notifications, [key]: !prev.notifications[key] } }))
  }

  return (
    <main className="onboarding-page">
      <header className="onboarding-header"><button aria-label="뒤로 가기" onClick={goBack}>←</button><strong>회원가입</strong><span /></header>
      <Progress step={step} />
      <section className="onboarding-content">
        {step === 1 && <>
          <div className="account-card"><span className="kakao-account-icon">K</span><div><strong>카카오 계정 연결 완료</strong><small>카카오 계정으로 안전하게 연결되었습니다.</small></div></div>
          <p className="eyebrow">프로필 설정</p><h1>러닝올레에서 어떻게 불러드릴까요?</h1>
          <label className="photo-picker">
            <input type="file" accept="image/*" onChange={selectPhoto} />
            <span className="photo-preview" style={form.profileImageUrl ? { backgroundImage: `url(${form.profileImageUrl})` } : undefined}>{!form.profileImageUrl && 'O'}</span>
            <em>+</em><b>사진 선택</b>
          </label>
          <label className="field"><span>닉네임</span><input maxLength={100} value={form.nickname} onChange={(event) => setForm({ ...form, nickname: event.target.value })} placeholder="러너제주" /></label>
          <p className={`field-help ${nicknameStatus === 'taken' ? 'invalid' : ''}`}>{nicknameStatus === 'checking' ? '닉네임을 확인하고 있습니다.' : nicknameStatus === 'taken' ? '이미 사용 중인 닉네임입니다.' : nicknameStatus === 'available' ? '사용 가능한 닉네임입니다.' : '2자 이상 입력해 주세요.'}</p>
          <label className="field"><span>자기소개 <small>{form.bio.length} / 100</small></span><textarea maxLength={100} value={form.bio} onChange={(event) => setForm({ ...form, bio: event.target.value })} placeholder="제주 바다와 오름을 좋아하는 러너입니다." /></label>
        </>}

        {step === 2 && <>
          <p className="eyebrow">러닝 취향</p><h1>어떤 스타일로 달리시나요?</h1>
          <div className="choice-group"><h2>사용자 유형 <small>복수 선택</small></h2><div className="choices">
            <Choice value="ACTIVE_RUNNER" emoji="A" label="활동적인 러너" selected={form.userTypes.includes('ACTIVE_RUNNER')} onClick={toggleUserType} />
            <Choice value="RELAXED_TRAVELER" emoji="T" label="여유로운 여행자" selected={form.userTypes.includes('RELAXED_TRAVELER')} onClick={toggleUserType} />
            <Choice value="JEJU_RESIDENT" emoji="J" label="제주 거주민" selected={form.userTypes.includes('JEJU_RESIDENT')} onClick={toggleUserType} />
          </div></div>
          <div className="choice-group"><h2>선호 거리</h2><div className="choices">
            <Choice value="UNDER_3KM" label="3km 이하" selected={form.preferredDistance === 'UNDER_3KM'} onClick={(value) => setForm({ ...form, preferredDistance: value })} />
            <Choice value="FROM_5_TO_10KM" label="5~10km" selected={form.preferredDistance === 'FROM_5_TO_10KM'} onClick={(value) => setForm({ ...form, preferredDistance: value })} />
            <Choice value="OVER_10KM" label="10km 이상" selected={form.preferredDistance === 'OVER_10KM'} onClick={(value) => setForm({ ...form, preferredDistance: value })} />
          </div></div>
          <div className="choice-group"><h2>선호 난이도</h2><div className="choices">
            <Choice value="EASY" label="쉬움" selected={form.preferredDifficulty === 'EASY'} onClick={(value) => setForm({ ...form, preferredDifficulty: value })} />
            <Choice value="NORMAL" label="보통" selected={form.preferredDifficulty === 'NORMAL'} onClick={(value) => setForm({ ...form, preferredDifficulty: value })} />
            <Choice value="HARD" label="어려움" selected={form.preferredDifficulty === 'HARD'} onClick={(value) => setForm({ ...form, preferredDifficulty: value })} />
          </div></div>
          <div className="choice-group"><h2>관심 테마 <small>복수 선택</small></h2><div className="choices">
            {themes.map((theme) => (
              <Choice
                key={theme.id}
                value={theme.id}
                label={themeLabels[theme.code] || theme.name}
                selected={form.themeIds.includes(theme.id)}
                onClick={toggleTheme}
              />
            ))}
          </div></div>
        </>}

        {step === 3 && <>
          <p className="eyebrow">마지막 단계</p><h1>약관 동의와 알림 설정</h1>
          <button className={`agree-all ${allTerms ? 'checked' : ''}`} onClick={() => { const nextValue = !allTerms; setForm((prev) => ({ ...prev, terms: { service: nextValue, privacy: nextValue, location: nextValue, marketing: nextValue } })) }}><i>✓</i> 전체 동의</button>
          <div className="term-list">
            {([['service', '(필수) 서비스 이용약관 동의'], ['privacy', '(필수) 개인정보 처리 동의'], ['location', '(필수) 위치정보 이용 동의'], ['marketing', '(선택) 마케팅 수신 동의']] as const).map(([key, label]) => <button key={key} onClick={() => setTerm(key, !form.terms[key])}><i className={form.terms[key] ? 'checked' : ''}>✓</i><span>{label}</span><b>›</b></button>)}
          </div>
          <hr /><h2 className="notification-title">알림 설정</h2>
          <div className="notification-box">
            {([['meetupInvite', '번개 참여·일정 알림'], ['commentLike', '댓글·좋아요 알림']] as const).map(([key, label]) => <div key={key}><span>{label}</span><Toggle checked={form.notifications[key]} onChange={() => setNotification(key)} /></div>)}
          </div>
        </>}
        {error && <p className="form-error">{error}</p>}
      </section>
      <footer className="onboarding-footer"><button disabled={!stepValid || submitting} onClick={next}>{submitting ? '저장 중...' : step === 3 ? '러닝올레 시작하기' : '다음'}</button></footer>
    </main>
  )
}

function isApiError(error: unknown): error is { response?: { data?: { message?: string } } } {
  return typeof error === 'object' && error !== null && 'response' in error
}
