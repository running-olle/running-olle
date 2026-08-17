import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { CourseReviewPage } from './pages/CourseReviewPage'
import { CourseReviewWorkspacePage } from './pages/CourseReviewWorkspacePage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<CourseReviewWorkspacePage />} />
        <Route path="/courses/:courseId" element={<CourseReviewPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
