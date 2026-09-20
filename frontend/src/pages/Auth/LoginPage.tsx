import { useCallback, useEffect, useRef, useState } from 'react'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const OAUTH_RECOVERY_KEY = 'runningOlleOAuthRecoveryAttempted'

const errorMessages: Record<string, string> = {
  authorization_request_not_found: '로그인 연결이 만료되었습니다. 카카오 로그인을 다시 시도해 주세요.',
  oauth_login_failed: '카카오 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.',
  access_denied: '카카오 로그인이 취소되었습니다.',
}

export function LoginPage() {
  const oauthError = new URLSearchParams(window.location.search).get('oauth_error')
  const [isRedirecting, setIsRedirecting] = useState(false)
  const redirectStartedRef = useRef(false)

  const startKakaoLogin = useCallback((isRecovery = false) => {
    if (redirectStartedRef.current) return
    redirectStartedRef.current = true
    setIsRedirecting(true)
    if (!isRecovery) sessionStorage.removeItem(OAUTH_RECOVERY_KEY)
    window.location.href = `${API_BASE_URL}/oauth2/authorization/kakao`
  }, [])

  useEffect(() => {
    if (oauthError !== 'authorization_request_not_found') return
    if (sessionStorage.getItem(OAUTH_RECOVERY_KEY)) return

    sessionStorage.setItem(OAUTH_RECOVERY_KEY, 'true')
    startKakaoLogin(true)
  }, [oauthError, startKakaoLogin])

  return (
    <main className="login-page">
      <div className="login-overlay" />
      <section className="login-copy">
        <div className="brand"><span className="brand-mark">🏃</span><strong>러닝올레</strong></div>
        <h1>제주를 달리며<br />여행하다 🌊</h1>
        <p>런트립의 새로운 시작, 러닝올레</p>
      </section>
      <section className="login-action">
        <h2>시작해볼까요?</h2>
        <p>카카오 계정으로 1초 만에 시작하세요</p>
        <button className="kakao-button" onClick={() => startKakaoLogin()} disabled={isRedirecting} aria-busy={isRedirecting}>
          <span className="kakao-icon">K</span> {isRedirecting ? '카카오로 이동 중…' : '카카오 로그인/시작하기'}
        </button>
        {oauthError && !isRedirecting && <p className="login-error">{errorMessages[oauthError] || errorMessages.oauth_login_failed}</p>}
        <small>로그인 후 이용약관과 개인정보 수집·이용 내용을 확인하고 동의할 수 있습니다.</small>
      </section>
    </main>
  )
}
