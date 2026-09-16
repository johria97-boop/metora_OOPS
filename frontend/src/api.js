import axios from 'axios'

const TOKEN_STORAGE_KEY = 'mentora.auth.token'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAccessToken()
      if (window.location.pathname !== '/auth') {
        window.location.replace('/auth')
      }
    }
    return Promise.reject(error)
  },
)

export function getAccessToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function storeAccessToken(token) {
  localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearAccessToken() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export function getApiErrorMessage(error, fallback) {
  return error.response?.data?.message ?? fallback
}

export default api
