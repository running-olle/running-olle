import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './components/auth/RequireAuth'
import { AppLayout } from './components/layout/AppLayout'
import { CurrentLocationProvider } from './features/home/CurrentLocationContext'
import { CurrentLocationLabel } from './features/home/CurrentLocationLabel'
import { LoginPage } from './pages/Auth/LoginPage'
import { OAuthCallbackPage } from './pages/Auth/OAuthCallbackPage'
import { OnboardingPage } from './pages/Auth/OnboardingPage'
import { CommunityPage } from './pages/Community/CommunityPage'
import { CourseBuilderPage } from './pages/Courses/CourseBuilderPage'
import { CourseDetailPage } from './pages/Courses/CourseDetailPage'
import { CourseSaveDetailPage } from './pages/Courses/CourseSaveDetailPage'
import { CoursesPage } from './pages/Courses/CoursesPage'
import { HomePage } from './pages/Home/HomePage'
import { AccountPage, BookmarksPage, CompletedRunsPage, MyPage, NotificationPage, ProfileEditPage, ReportsPage, RunningHistoryPage, SettingsPage, TripCreatePage, TripsPage, VisitedPlacesPage } from './pages/MyPage/MyPage'
import { FreeRunReadyPage } from './pages/Running/FreeRunReadyPage'
import { LiveRunningPage } from './pages/Running/LiveRunningPage'
import { RunningCourseSelectPage } from './pages/Running/RunningCourseSelectPage'
import { RunningCompletePage } from './pages/Running/RunningCompletePage'
import { RunningSelectPage } from './pages/Running/RunningSelectPage'
import { RunningRecordDetailPage } from './pages/MyPage/RunningRecordDetailPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      {import.meta.env.DEV && (
        <Route element={<AppLayout />}>
          <Route path="/dev/mypage" element={<MyPage />} />
          <Route path="/dev/mypage/history" element={<RunningHistoryPage />} />
          <Route path="/dev/mypage/history/visits" element={<VisitedPlacesPage />} />
        </Route>
      )}

      <Route element={<RequireAuth onboarding="incomplete" />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
      </Route>

      <Route element={<RequireAuth onboarding="required" />}>
        <Route element={(
          <CurrentLocationProvider>
            <AppLayout leftSlot={<CurrentLocationLabel />} />
          </CurrentLocationProvider>
        )}>
          <Route path="/" element={<HomePage />} />
          <Route path="/courses" element={<CoursesPage />} />
          <Route path="/courses/:courseId" element={<CourseDetailPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/mypage/history" element={<RunningHistoryPage />} />
          <Route path="/mypage/history/all" element={<CompletedRunsPage />} />
          <Route path="/mypage/history/visits" element={<VisitedPlacesPage />} />
          <Route path="/mypage/history/:recordId" element={<RunningRecordDetailPage />} />
          <Route path="/mypage/bookmarks" element={<BookmarksPage />} />
          <Route path="/mypage/reports" element={<ReportsPage />} />
          <Route path="/mypage/trips" element={<TripsPage />} />
          <Route path="/mypage/trips/new" element={<TripCreatePage />} />
          <Route path="/mypage/settings" element={<SettingsPage />} />
          <Route path="/mypage/settings/account" element={<AccountPage />} />
          <Route path="/mypage/settings/profile" element={<ProfileEditPage />} />
          <Route path="/mypage/settings/notifications" element={<NotificationPage />} />
          <Route path="/running" element={<RunningSelectPage />} />
          <Route path="/running/courses" element={<RunningCourseSelectPage />} />
        </Route>
        <Route path="/courses/create" element={<CourseBuilderPage />} />
        <Route path="/courses/create/save" element={<CourseSaveDetailPage />} />
        <Route path="/running/free" element={<FreeRunReadyPage />} />
        <Route path="/running/live" element={<LiveRunningPage />} />
        <Route path="/running/complete" element={<RunningCompletePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
