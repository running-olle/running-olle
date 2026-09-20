import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

const TOKEN_KEY = 'runningOlleAccessToken'
const OAUTH_RECOVERY_KEY = 'runningOlleOAuthRecoveryAttempted'

export function OAuthCallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState('')

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1))
    const token = params.get('access_token')
    const completed = params.get('onboarding_completed') === 'true'

    if (!token) {
      setError('카카오 로그인 정보를 확인할 수 없습니다.')
      return
    }

    localStorage.setItem(TOKEN_KEY, token)
    sessionStorage.removeItem(OAUTH_RECOVERY_KEY)
    navigate(completed ? '/' : '/onboarding', { replace: true })
  }, [navigate])

  return <main className="center-page"><div className="spinner" /><p>{error || '카카오 로그인 중입니다…'}</p></main>
}
