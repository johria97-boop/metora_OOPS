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
    const publicAuthRequest = ['/auth/login', '/auth/register'].includes(error.config?.url)
    if (error.response?.status === 401 && !publicAuthRequest) {
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

export function getAuthErrorMessage(error, mode) {
  if (!error.response) {
    return 'Unable to connect to the server. Please try again.'
  }

  const { status, data } = error.response
  if (status === 401) {
    return 'Invalid email or password.'
  }
  if (status === 400) {
    return typeof data?.message === 'string' && data.message.trim()
      ? data.message
      : 'Please check the information you entered.'
  }
  if (status >= 500) {
    return 'Something went wrong on the server. Please try again.'
  }
  if (mode === 'login') {
    return 'Something went wrong. Please try again.'
  }

  return typeof data?.message === 'string' && data.message.trim()
    ? data.message
    : 'Something went wrong. Please try again.'
}

export default api
