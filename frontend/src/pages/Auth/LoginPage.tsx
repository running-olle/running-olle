import { useCallback, useEffect, useRef, useState } from 'react'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const OAUTH_LOGIN_LOCK_KEY = 'runningOlleOAuthLoginStartedAt'
const OAUTH_LOGIN_LOCK_MS = 10_000
const OAUTH_RATE_LIMIT_UNTIL_KEY = 'runningOlleOAuthRateLimitUntil'
const OAUTH_RATE_LIMIT_COOLDOWN_MS = 60_000

const errorMessages: Record<string, string> = {
  authorization_request_not_found: '로그인 연결이 만료되었습니다. 카카오 로그인을 다시 시도해 주세요.',
  authorization_code_invalid: '카카오 로그인 정보가 만료되었습니다. 새로 로그인을 시작해 주세요.',
  oauth_rate_limited: '로그인 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
  oauth_provider_unavailable: '카카오 로그인 서버에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.',
  oauth_token_exchange_failed: '카카오 인증 처리에 실패했습니다. 새로 로그인을 시작해 주세요.',
  oauth_configuration_error: '카카오 로그인 설정을 확인해야 합니다. 관리자에게 문의해 주세요.',
  oauth_login_failed: '카카오 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.',
  access_denied: '카카오 로그인이 취소되었습니다.',
}

export function LoginPage() {
  const oauthError = new URLSearchParams(window.location.search).get('oauth_error')
  const oauthTrace = new URLSearchParams(window.location.search).get('oauth_trace')
  const [isRedirecting, setIsRedirecting] = useState(false)
  const [localError, setLocalError] = useState('')
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)
  const redirectStartedRef = useRef(false)

  useEffect(() => {
    const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
    const previousThemeColor = themeColor?.content

    themeColor?.setAttribute('content', '#5f9fc5')

    return () => {
      if (themeColor && previousThemeColor) {
        themeColor.content = previousThemeColor
      }
    }
  }, [])

  useEffect(() => {
    if (!oauthError) return
    localStorage.removeItem(OAUTH_LOGIN_LOCK_KEY)

    if (oauthError !== 'oauth_rate_limited') return
    const now = Date.now()
    const storedUntil = Number(localStorage.getItem(OAUTH_RATE_LIMIT_UNTIL_KEY) || 0)
    const blockedUntil = storedUntil > now ? storedUntil : now + OAUTH_RATE_LIMIT_COOLDOWN_MS
    localStorage.setItem(OAUTH_RATE_LIMIT_UNTIL_KEY, String(blockedUntil))

    const updateCountdown = () => {
      const seconds = Math.max(0, Math.ceil((blockedUntil - Date.now()) / 1000))
      setRateLimitSeconds(seconds)
      if (seconds === 0) localStorage.removeItem(OAUTH_RATE_LIMIT_UNTIL_KEY)
    }
    updateCountdown()
    const interval = window.setInterval(updateCountdown, 1_000)
    return () => window.clearInterval(interval)
  }, [oauthError])

  const startKakaoLogin = useCallback(() => {
    if (redirectStartedRef.current) return

    const now = Date.now()
    const rateLimitUntil = Number(localStorage.getItem(OAUTH_RATE_LIMIT_UNTIL_KEY) || 0)
    if (rateLimitUntil > now) {
      setRateLimitSeconds(Math.ceil((rateLimitUntil - now) / 1000))
      return
    }
    const previousAttempt = Number(localStorage.getItem(OAUTH_LOGIN_LOCK_KEY) || 0)
    if (now - previousAttempt < OAUTH_LOGIN_LOCK_MS) {
      setLocalError('로그인 요청을 처리하고 있습니다. 잠시만 기다려 주세요.')
      return
    }

    redirectStartedRef.current = true
    setIsRedirecting(true)
    setLocalError('')
    localStorage.setItem(OAUTH_LOGIN_LOCK_KEY, String(now))
    window.location.href = `${API_BASE_URL}/oauth2/authorization/kakao`
  }, [])

  return (
    <main className="login-page">
      <div className="login-overlay" />
      <section className="login-copy">
        <div className="brand"><img className="brand-mark" src="/images/running-olle-logo.png" alt="" /><strong>러닝올레</strong></div>
        <h1>제주를 달리며<br />여행하다</h1>
        <p>제주도로 러닝하러 올레?</p>
      </section>
      <section className="login-action">
        <h2>시작해볼까요?</h2>
        <p>카카오 계정으로 1초 만에 시작하세요</p>
        <button className="kakao-button" onClick={startKakaoLogin} disabled={isRedirecting || rateLimitSeconds > 0} aria-busy={isRedirecting} aria-label="카카오 로그인">
          <img src="/images/kakao-login.png" alt="" />
        </button>
        {(localError || oauthError) && !isRedirecting && (
          <p className="login-error">
            {localError || errorMessages[oauthError || ''] || errorMessages.oauth_login_failed}
            {rateLimitSeconds > 0 && ` (최소 ${rateLimitSeconds}초 후 다시 시도해 주세요)`}
            {oauthTrace && <small>오류 ID: {oauthTrace}</small>}
          </p>
        )}
        <small>로그인 후 이용약관과 개인정보 수집·이용 내용을 확인하고 동의할 수 있습니다.</small>
      </section>
    </main>
  )
}
