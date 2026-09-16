import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth, RequireGuest } from './auth.jsx'
import AuthPage from './pages/Auth.jsx'
import AcademicsPage from './pages/Academics.jsx'
import ProfilePage from './pages/Profile.jsx'
import ProductivityPage from './pages/Productivity.jsx'
import CareerPage from './pages/Career.jsx'
import RecommendationsPage from './pages/Recommendations.jsx'
import DashboardPage from './pages/Dashboard.jsx'
import RoadmapsPage from './pages/Roadmaps.jsx'

export default function App() {
  return (
    <Routes>
      <Route
        path="/auth"
        element={
          <RequireGuest>
            <AuthPage />
          </RequireGuest>
        }
      />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        }
      />
      <Route
        path="/profile"
        element={
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        }
      />
      <Route
        path="/academics"
        element={
          <RequireAuth>
            <AcademicsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/productivity"
        element={
          <RequireAuth>
            <ProductivityPage />
          </RequireAuth>
        }
      />
      <Route
        path="/career"
        element={
          <RequireAuth>
            <CareerPage />
          </RequireAuth>
        }
      />
      <Route
        path="/recommendations"
        element={
          <RequireAuth>
            <RecommendationsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/roadmaps"
        element={
          <RequireAuth>
            <RoadmapsPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
