import { useEffect, useMemo, useState } from 'react'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

const initialForm = { courseName: '', currentLevel: 'BEGINNER', targetWeeks: '' }

export default function RoadmapsPage() {
  const [roadmaps, setRoadmaps] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [form, setForm] = useState(initialForm)
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [updatingStep, setUpdatingStep] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    api.get('/roadmaps')
      .then(({ data }) => {
        if (!active) return
        setRoadmaps(data)
        setSelectedId(data[0]?.id ?? null)
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, 'Unable to load roadmaps.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  const selected = roadmaps.find((roadmap) => roadmap.id === selectedId) ?? null
  const phases = useMemo(() => {
    if (!selected) return []
    const grouped = new Map()
    selected.steps.forEach((step) => {
      const group = grouped.get(step.phaseNumber) ?? {
        phaseNumber: step.phaseNumber,
        phaseTitle: step.phaseTitle,
        steps: [],
      }
      group.steps.push(step)
      grouped.set(step.phaseNumber, group)
    })
    return [...grouped.values()]
  }, [selected])

  function beginAction() {
    setError('')
    setMessage('')
  }

  async function generateRoadmap(event) {
    event.preventDefault()
    beginAction()
    setGenerating(true)
    const payload = {
      courseName: form.courseName,
      currentLevel: form.currentLevel,
      ...(form.targetWeeks === '' ? {} : { targetWeeks: Number(form.targetWeeks) }),
    }
    try {
      const { data } = await api.post('/roadmaps/generate', payload)
      setRoadmaps((current) => [data, ...current])
      setSelectedId(data.id)
      setForm(initialForm)
      setMessage('Roadmap generated and saved.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to generate the roadmap.'))
    } finally {
      setGenerating(false)
    }
  }

  async function updateStatus(stepId, status) {
    beginAction()
    setUpdatingStep(stepId)
    try {
      const { data } = await api.patch(
        `/roadmaps/${selected.id}/steps/${stepId}/status`,
        { status },
      )
      setRoadmaps((current) => current.map((item) => item.id === data.id ? data : item))
      setMessage('Step progress saved.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to update step progress.'))
    } finally {
      setUpdatingStep(null)
    }
  }

  async function deleteRoadmap() {
    if (!selected || !window.confirm(`Delete the ${selected.courseName} roadmap?`)) return
    beginAction()
    try {
      await api.delete(`/roadmaps/${selected.id}`)
      const remaining = roadmaps.filter((item) => item.id !== selected.id)
      setRoadmaps(remaining)
      setSelectedId(remaining[0]?.id ?? null)
      setMessage('Roadmap deleted.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to delete the roadmap.'))
    }
  }

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        <header className="mb-4">
          <p className="text-uppercase fw-semibold text-primary small mb-1">AI learning planner</p>
          <h1 className="h2">Learning roadmaps</h1>
          <p className="text-secondary mb-0">Generate a roadmap once, then continue your saved progress anytime.</p>
        </header>

        {message && <div className="alert alert-success py-2">{message}</div>}
        {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}

        <section className="roadmap-panel rounded-4 shadow-sm p-4 mb-4">
          <h2 className="h5 mb-3">Generate a roadmap</h2>
          <form onSubmit={generateRoadmap}>
            <div className="row g-3 align-items-end">
              <div className="col-lg-5">
                <label className="form-label" htmlFor="roadmap-course">Course or skill</label>
                <input id="roadmap-course" className="form-control" maxLength="120" required value={form.courseName} onChange={(event) => setForm({ ...form, courseName: event.target.value })} placeholder="Java Backend Development" />
              </div>
              <div className="col-sm-6 col-lg-3">
                <label className="form-label" htmlFor="roadmap-level">Current level</label>
                <select id="roadmap-level" className="form-select" value={form.currentLevel} onChange={(event) => setForm({ ...form, currentLevel: event.target.value })}>
                  <option value="BEGINNER">Beginner</option>
                  <option value="INTERMEDIATE">Intermediate</option>
                  <option value="ADVANCED">Advanced</option>
                </select>
              </div>
              <div className="col-sm-6 col-lg-2">
                <label className="form-label" htmlFor="roadmap-weeks">Target weeks</label>
                <input id="roadmap-weeks" className="form-control" type="number" min="1" max="52" value={form.targetWeeks} onChange={(event) => setForm({ ...form, targetWeeks: event.target.value })} placeholder="Optional" />
              </div>
              <div className="col-lg-2 d-grid">
                <button className="btn btn-primary" type="submit" disabled={generating}>
                  {generating ? 'Generating...' : 'Generate roadmap'}
                </button>
              </div>
            </div>
          </form>
        </section>

        {loading && <div className="text-center py-5">Loading roadmaps...</div>}

        {!loading && roadmaps.length === 0 && (
          <section className="roadmap-panel rounded-4 shadow-sm p-4 text-center">
            <h2 className="h5">No saved roadmaps</h2>
            <p className="text-secondary mb-0">Enter a course or skill above to create your first learning path.</p>
          </section>
        )}

        {!loading && roadmaps.length > 0 && (
          <div className="row g-4">
            <aside className="col-lg-4">
              <section className="roadmap-panel rounded-4 shadow-sm p-3">
                <h2 className="h6 px-2 pt-2">Saved roadmaps</h2>
                <div className="list-group list-group-flush">
                  {roadmaps.map((roadmap) => (
                    <button className={`list-group-item list-group-item-action rounded-3 my-1 ${roadmap.id === selectedId ? 'active' : ''}`} type="button" key={roadmap.id} onClick={() => setSelectedId(roadmap.id)}>
                      <span className="d-flex justify-content-between gap-2"><strong>{roadmap.courseName}</strong><span>{roadmap.progress.toFixed(0)}%</span></span>
                      <small>{roadmap.completedSteps}/{roadmap.totalSteps} steps - {roadmap.state}</small>
                    </button>
                  ))}
                </div>
              </section>
            </aside>

            {selected && (
              <div className="col-lg-8">
                <section className="roadmap-panel rounded-4 shadow-sm p-4">
                  <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                    <div>
                      <p className="text-uppercase text-primary fw-semibold small mb-1">{selected.currentLevel}{selected.targetWeeks ? ` - ${selected.targetWeeks} weeks` : ''}</p>
                      <h2 className="h3">{selected.courseName}</h2>
                    </div>
                    <button className="btn btn-sm btn-outline-danger" type="button" onClick={deleteRoadmap}>Delete</button>
                  </div>
                  <p className="text-secondary">{selected.summary}</p>
                  <div className="d-flex justify-content-between small mb-1"><strong>Overall progress</strong><span>{selected.completedSteps}/{selected.totalSteps} steps - {selected.progress.toFixed(1)}%</span></div>
                  <div className="progress mb-4" role="progressbar" aria-label={`${selected.courseName} progress`} aria-valuenow={selected.progress} aria-valuemin="0" aria-valuemax="100">
                    <div className="progress-bar bg-success" style={{ width: `${selected.progress}%` }} />
                  </div>

                  {phases.map((phase) => (
                    <section className="roadmap-phase mb-4" key={phase.phaseNumber}>
                      <h3 className="h5 border-bottom pb-2">Phase {phase.phaseNumber} - {phase.phaseTitle}</h3>
                      <div className="vstack gap-3">
                        {phase.steps.map((step) => (
                          <article className={`roadmap-step border rounded-3 p-3 status-${step.status.toLowerCase()}`} key={step.id}>
                            <div className="d-flex flex-wrap justify-content-between gap-3">
                              <div className="flex-grow-1">
                                <h4 className="h6 mb-1">{step.position}. {step.title}</h4>
                                <p className="text-secondary small mb-1">{step.description}</p>
                                <small className="text-secondary">Estimated {step.estimatedHours} hour{step.estimatedHours === 1 ? '' : 's'}</small>
                              </div>
                              <div>
                                <label className="visually-hidden" htmlFor={`step-status-${step.id}`}>Status for {step.title}</label>
                                <select id={`step-status-${step.id}`} className="form-select form-select-sm" value={step.status} disabled={updatingStep === step.id} onChange={(event) => updateStatus(step.id, event.target.value)}>
                                  <option value="NOT_STARTED">Not started</option>
                                  <option value="IN_PROGRESS">In progress</option>
                                  <option value="COMPLETED">Completed</option>
                                </select>
                              </div>
                            </div>
                          </article>
                        ))}
                      </div>
                    </section>
                  ))}
                </section>
              </div>
            )}
          </div>
        )}
      </main>
    </>
  )
}
