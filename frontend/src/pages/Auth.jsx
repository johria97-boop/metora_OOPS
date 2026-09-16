import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { getApiErrorMessage } from '../api.js'
import { useAuth } from '../auth.jsx'

const emptyForm = {
  email: '',
  password: '',
  name: '',
  branch: '',
  semester: 1,
}

export default function AuthPage() {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const { signIn } = useAuth()
  const navigate = useNavigate()

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  function changeMode(nextMode) {
    setMode(nextMode)
    setError('')
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    const payload = mode === 'register'
      ? { ...form, semester: Number(form.semester) }
      : { email: form.email, password: form.password }

    try {
      const response = await api.post(`/auth/${mode}`, payload)
      signIn(response.data.token)
      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to authenticate. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="container py-5">
      <section className="auth-card mx-auto rounded-4 shadow-sm overflow-hidden">
        <div className="row g-0">
          <div className="col-md-5 brand-panel p-4 p-lg-5 text-white">
            <p className="text-uppercase fw-semibold small mb-2">Personalized mentoring</p>
            <h1 className="display-6 fw-bold">MENTORA</h1>
            <p className="mb-0 opacity-75">Your student profile is the starting point for a focused learning journey.</p>
          </div>
          <div className="col-md-7 bg-white p-4 p-lg-5">
            <div className="btn-group w-100 mb-4" role="group" aria-label="Authentication mode">
              <button
                type="button"
                className={`btn ${mode === 'login' ? 'btn-primary' : 'btn-outline-primary'}`}
                onClick={() => changeMode('login')}
              >
                Login
              </button>
              <button
                type="button"
                className={`btn ${mode === 'register' ? 'btn-primary' : 'btn-outline-primary'}`}
                onClick={() => changeMode('register')}
              >
                Register
              </button>
            </div>

            <h2 className="h4 mb-3">{mode === 'login' ? 'Welcome back' : 'Create student account'}</h2>
            {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}

            <form onSubmit={submit}>
              {mode === 'register' && (
                <>
                  <label className="form-label" htmlFor="name">Name</label>
                  <input className="form-control mb-3" id="name" name="name" value={form.name} onChange={updateField} maxLength="100" required />

                  <label className="form-label" htmlFor="branch">Branch</label>
                  <input className="form-control mb-3" id="branch" name="branch" value={form.branch} onChange={updateField} maxLength="100" required />

                  <label className="form-label" htmlFor="semester">Semester</label>
                  <input className="form-control mb-3" id="semester" name="semester" type="number" min="1" max="12" value={form.semester} onChange={updateField} required />
                </>
              )}

              <label className="form-label" htmlFor="email">Email</label>
              <input className="form-control mb-3" id="email" name="email" type="email" autoComplete="email" value={form.email} onChange={updateField} maxLength="254" required />

              <label className="form-label" htmlFor="password">Password</label>
              <input className="form-control mb-4" id="password" name="password" type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={form.password} onChange={updateField} minLength="8" maxLength="72" required />

              <button className="btn btn-primary w-100" type="submit" disabled={submitting}>
                {submitting ? 'Please wait…' : mode === 'login' ? 'Login' : 'Register'}
              </button>
            </form>
          </div>
        </div>
      </section>
    </main>
  )
}
