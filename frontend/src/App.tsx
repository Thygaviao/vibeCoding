import { Navigate, Route, Routes } from 'react-router-dom'
import { MainLayout } from './layouts/MainLayout'
import { SubmissionPage } from './pages/public/SubmissionPage'
import { SuccessPage } from './pages/public/SuccessPage'
import { AdminLoginPage } from './pages/admin/AdminLoginPage'
import { SubmissionsPage } from './pages/admin/SubmissionsPage'
import { SubmissionDetailPage } from './pages/admin/SubmissionDetailPage'

export function App() {
  return (
    <MainLayout>
      <Routes>
        <Route path="/" element={<SubmissionPage />} />
        <Route path="/success/:id" element={<SuccessPage />} />
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route path="/admin/submissions" element={<SubmissionsPage />} />
        <Route path="/admin/submissions/:id" element={<SubmissionDetailPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </MainLayout>
  )
}
