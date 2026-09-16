import { createContext, useContext, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { clearAccessToken, getAccessToken, storeAccessToken } from './api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(getAccessToken)

  function signIn(nextToken) {
    storeAccessToken(nextToken)
    setToken(nextToken)
  }

  function signOut() {
    clearAccessToken()
    setToken(null)
  }

  return (
    <AuthContext.Provider value={{ token, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}

export function RequireAuth({ children }) {
  const { token } = useAuth()
  const location = useLocation()
  return token ? children : <Navigate to="/auth" state={{ from: location }} replace />
}

export function RequireGuest({ children }) {
  const { token } = useAuth()
  return token ? <Navigate to="/dashboard" replace /> : children
}
