import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

export default function ProfilePage() {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({ name: '', branch: '', semester: 1 })
  const [editing, setEditing] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    api.get('/student/me')
      .then(({ data }) => {
        if (active) {
          setProfile(data)
          setForm({ name: data.name, branch: data.branch, semester: data.semester })
        }
      })
      .catch((requestError) => {
        if (!active) return
        setError(getApiErrorMessage(requestError, 'Unable to load your profile.'))
      })

    return () => {
      active = false
    }
  }, [])

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function save(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      const { data } = await api.put('/student/me', { ...form, semester: Number(form.semester) })
      setProfile(data)
      setForm({ name: data.name, branch: data.branch, semester: data.semester })
      setEditing(false)
      setMessage('Profile updated successfully.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to update your profile.'))
    }
  }

  if (!profile && !error) {
    return <main className="container py-5 text-center">Loading profile…</main>
  }

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        <section className="profile-card mx-auto bg-white rounded-4 shadow-sm p-4 p-md-5">
          <header className="mb-4">
          <div>
            <p className="text-uppercase fw-semibold text-primary small mb-1">Student profile</p>
            <h1 className="h2 mb-1">{profile?.name ?? 'MENTORA'}</h1>
            <p className="text-secondary mb-0">{profile?.email}</p>
          </div>
          </header>

        {message && <div className="alert alert-success py-2">{message}</div>}
        {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}

        {profile && (
          <form onSubmit={save}>
            <label className="form-label" htmlFor="name">Name</label>
            <input className="form-control mb-3" id="name" name="name" value={form.name} onChange={updateField} maxLength="100" disabled={!editing} required />

            <label className="form-label" htmlFor="branch">Branch</label>
            <input className="form-control mb-3" id="branch" name="branch" value={form.branch} onChange={updateField} maxLength="100" disabled={!editing} required />

            <label className="form-label" htmlFor="semester">Semester</label>
            <input className="form-control mb-4" id="semester" name="semester" type="number" min="1" max="12" value={form.semester} onChange={updateField} disabled={!editing} required />

            {editing ? (
              <div className="d-flex gap-2">
                <button className="btn btn-primary" type="submit">Save profile</button>
                <button className="btn btn-outline-secondary" type="button" onClick={() => {
                  setForm({ name: profile.name, branch: profile.branch, semester: profile.semester })
                  setEditing(false)
                }}>
                  Cancel
                </button>
              </div>
            ) : (
              <button className="btn btn-primary" type="button" onClick={() => setEditing(true)}>Edit profile</button>
            )}
          </form>
        )}
        </section>
      </main>
    </>
  )
}
