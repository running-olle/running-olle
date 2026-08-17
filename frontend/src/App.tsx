import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './components/auth/RequireAuth'
import { AppLayout } from './components/layout/AppLayout'
import { user } from './mocks/home'
import { LoginPage } from './pages/Auth/LoginPage'
import { OAuthCallbackPage } from './pages/Auth/OAuthCallbackPage'
import { OnboardingPage } from './pages/Auth/OnboardingPage'
import { CommunityPage } from './pages/Community/CommunityPage'
import { CourseReviewPage } from './pages/CourseReviewPage'
import { CoursesPage } from './pages/Courses/CoursesPage'
import { HomePage } from './pages/Home/HomePage'
import { MyPage } from './pages/MyPage/MyPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

      <Route element={<RequireAuth onboarding="incomplete" />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
      </Route>

      <Route element={<RequireAuth onboarding="required" />}>
        <Route element={<AppLayout leftSlot={<span>📍 {user.location}</span>} />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/courses" element={<CoursesPage />} />
          <Route path="/courses/:courseId/reviews" element={<CourseReviewPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/mypage" element={<MyPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
