import { useEffect, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { axiosInstance } from '../../api/axiosInstance'

const LEGACY_TOKEN_KEY = 'runningOlleAccessToken'

type OnboardingRequirement = 'required' | 'incomplete' | 'none'

type CurrentUserResponse = {
  onboardingCompleted: boolean
}

type Session = {
  authenticated: boolean
  onboardingCompleted: boolean
  validatedFor: OnboardingRequirement
}

export function RequireAuth({ onboarding = 'none' }: { onboarding?: OnboardingRequirement }) {
  const [session, setSession] = useState<Session | null>(null)

  useEffect(() => {
    let active = true
    setSession(null)
    localStorage.removeItem(LEGACY_TOKEN_KEY)

    axiosInstance.get<CurrentUserResponse>('/users/me')
      .then(({ data }) => {
        if (active) {
          setSession({
            authenticated: true,
            onboardingCompleted: data.onboardingCompleted,
            validatedFor: onboarding,
          })
        }
      })
      .catch(() => {
        if (active) {
          setSession({ authenticated: false, onboardingCompleted: false, validatedFor: onboarding })
        }
      })

    return () => {
      active = false
    }
  }, [onboarding])

  if (session === null || session.validatedFor !== onboarding) {
    return <main className="center-page"><div className="spinner" /><p>로그인 정보를 확인하고 있습니다…</p></main>
  }

  if (!session.authenticated) return <Navigate to="/login" replace />
  if (onboarding === 'required' && !session.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />
  }
  if (onboarding === 'incomplete' && session.onboardingCompleted) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
