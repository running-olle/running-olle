import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { axiosInstance } from '../../api/axiosInstance'

const LEGACY_TOKEN_KEY = 'runningOlleAccessToken'
const OAUTH_LOGIN_LOCK_KEY = 'runningOlleOAuthLoginStartedAt'
const OAUTH_RATE_LIMIT_UNTIL_KEY = 'runningOlleOAuthRateLimitUntil'

type CurrentUserResponse = {
  onboardingCompleted: boolean
}

export function OAuthCallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState('')

  useEffect(() => {
    localStorage.removeItem(LEGACY_TOKEN_KEY)
    localStorage.removeItem(OAUTH_LOGIN_LOCK_KEY)
    localStorage.removeItem(OAUTH_RATE_LIMIT_UNTIL_KEY)

    axiosInstance.get<CurrentUserResponse>('/users/me')
      .then(({ data }) => navigate(data.onboardingCompleted ? '/' : '/onboarding', { replace: true }))
      .catch(() => setError('카카오 로그인 정보를 확인할 수 없습니다. 다시 로그인해 주세요.'))
  }, [navigate])

  return <main className="center-page"><div className="spinner" /><p>{error || '카카오 로그인 중입니다…'}</p></main>
}
