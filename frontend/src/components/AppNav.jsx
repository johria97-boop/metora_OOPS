import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function AppNav() {
  const { signOut } = useAuth()
  const navigate = useNavigate()

  function logout() {
    signOut()
    navigate('/auth', { replace: true })
  }

  return (
    <nav className="navbar navbar-expand bg-white border-bottom shadow-sm">
      <div className="container">
        <NavLink className="navbar-brand fw-bold text-primary" to="/dashboard">MENTORA</NavLink>
        <div className="d-flex flex-wrap justify-content-end align-items-center gap-2 gap-md-3">
          <NavLink className="nav-link" to="/dashboard">Dashboard</NavLink>
          <NavLink className="nav-link" to="/profile">Profile</NavLink>
          <NavLink className="nav-link" to="/academics">Academics</NavLink>
          <NavLink className="nav-link" to="/productivity">Productivity</NavLink>
          <NavLink className="nav-link" to="/career">Career</NavLink>
          <NavLink className="nav-link" to="/recommendations">Recommendations</NavLink>
          <NavLink className="nav-link" to="/roadmaps">Roadmaps</NavLink>
          <button className="btn btn-sm btn-outline-secondary" type="button" onClick={logout}>Logout</button>
        </div>
      </div>
    </nav>
  )
}
