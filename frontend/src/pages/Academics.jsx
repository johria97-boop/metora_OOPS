import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

const emptyRecord = { subject: '', score: '', semester: 1 }
const emptyPlan = { subject: '', targetHours: '', completedHours: 0 }

export default function AcademicsPage() {
  const [records, setRecords] = useState([])
  const [plans, setPlans] = useState([])
  const [recordForm, setRecordForm] = useState(emptyRecord)
  const [planForm, setPlanForm] = useState(emptyPlan)
  const [editingRecordId, setEditingRecordId] = useState(null)
  const [editingPlanId, setEditingPlanId] = useState(null)
  const [progressValues, setProgressValues] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  useEffect(() => {
    let active = true
    Promise.all([
      api.get('/academic/records'),
      api.get('/academic/study-plans'),
    ])
      .then(([recordResponse, planResponse]) => {
        if (!active) return
        setRecords(recordResponse.data)
        setPlans(planResponse.data)
        setProgressValues(Object.fromEntries(
          planResponse.data.map((plan) => [plan.id, plan.completedHours]),
        ))
      })
      .catch((requestError) => {
        if (active) handleRequestError(requestError, 'Unable to load academic data.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  function handleRequestError(requestError, fallback) {
    setError(getApiErrorMessage(requestError, fallback))
  }

  function beginAction() {
    setError('')
    setMessage('')
  }

  function resetRecordForm() {
    setRecordForm(emptyRecord)
    setEditingRecordId(null)
  }

  function resetPlanForm() {
    setPlanForm(emptyPlan)
    setEditingPlanId(null)
  }

  async function saveRecord(event) {
    event.preventDefault()
    beginAction()
    const payload = {
      subject: recordForm.subject,
      score: Number(recordForm.score),
      semester: Number(recordForm.semester),
    }

    try {
      if (editingRecordId) {
        const { data } = await api.put(`/academic/records/${editingRecordId}`, payload)
        setRecords((current) => current.map((record) => record.id === data.id ? data : record))
        setMessage('Academic record updated.')
      } else {
        const { data } = await api.post('/academic/records', payload)
        setRecords((current) => [...current, data])
        setMessage('Academic record added.')
      }
      resetRecordForm()
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the academic record.')
    }
  }

  function editRecord(record) {
    setEditingRecordId(record.id)
    setRecordForm({ subject: record.subject, score: record.score, semester: record.semester })
    beginAction()
  }

  async function removeRecord(id) {
    if (!window.confirm('Delete this academic record?')) return
    beginAction()
    try {
      await api.delete(`/academic/records/${id}`)
      setRecords((current) => current.filter((record) => record.id !== id))
      if (editingRecordId === id) resetRecordForm()
      setMessage('Academic record deleted.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to delete the academic record.')
    }
  }

  async function savePlan(event) {
    event.preventDefault()
    beginAction()
    const payload = {
      subject: planForm.subject,
      targetHours: Number(planForm.targetHours),
      completedHours: Number(planForm.completedHours),
    }

    try {
      let saved
      if (editingPlanId) {
        saved = (await api.put(`/academic/study-plans/${editingPlanId}`, payload)).data
        setPlans((current) => current.map((plan) => plan.id === saved.id ? saved : plan))
        setMessage('Study plan updated.')
      } else {
        saved = (await api.post('/academic/study-plans', payload)).data
        setPlans((current) => [...current, saved])
        setMessage('Study plan added.')
      }
      setProgressValues((current) => ({ ...current, [saved.id]: saved.completedHours }))
      resetPlanForm()
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the study plan.')
    }
  }

  function editPlan(plan) {
    setEditingPlanId(plan.id)
    setPlanForm({
      subject: plan.subject,
      targetHours: plan.targetHours,
      completedHours: plan.completedHours,
    })
    beginAction()
  }

  async function updateProgress(plan) {
    beginAction()
    try {
      const { data } = await api.patch(`/academic/study-plans/${plan.id}/progress`, {
        completedHours: Number(progressValues[plan.id]),
      })
      setPlans((current) => current.map((item) => item.id === data.id ? data : item))
      setProgressValues((current) => ({ ...current, [data.id]: data.completedHours }))
      setMessage('Study progress updated.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to update study progress.')
    }
  }

  async function removePlan(id) {
    if (!window.confirm('Delete this study plan?')) return
    beginAction()
    try {
      await api.delete(`/academic/study-plans/${id}`)
      setPlans((current) => current.filter((plan) => plan.id !== id))
      if (editingPlanId === id) resetPlanForm()
      setMessage('Study plan deleted.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to delete the study plan.')
    }
  }

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        <header className="mb-4">
          <p className="text-uppercase fw-semibold text-primary small mb-1">Academic module</p>
          <h1 className="h2">Records and study plans</h1>
          <p className="text-secondary mb-0">Track results and keep study progress within a clear target.</p>
        </header>

        {message && <div className="alert alert-success py-2">{message}</div>}
        {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}
        {loading && <div className="text-center py-5">Loading academics...</div>}

        {!loading && (
          <div className="row g-4">
            <div className="col-lg-6">
              <section className="academic-panel rounded-4 shadow-sm p-4 mb-4">
                <h2 className="h5 mb-3">{editingRecordId ? 'Edit academic record' : 'Add academic record'}</h2>
                <form onSubmit={saveRecord}>
                  <label className="form-label" htmlFor="record-subject">Subject</label>
                  <input className="form-control mb-3" id="record-subject" value={recordForm.subject} onChange={(event) => setRecordForm({ ...recordForm, subject: event.target.value })} maxLength="100" required />

                  <div className="row">
                    <div className="col-sm-6">
                      <label className="form-label" htmlFor="score">Score</label>
                      <input className="form-control mb-3" id="score" type="number" min="0" max="100" step="0.1" value={recordForm.score} onChange={(event) => setRecordForm({ ...recordForm, score: event.target.value })} required />
                    </div>
                    <div className="col-sm-6">
                      <label className="form-label" htmlFor="record-semester">Semester</label>
                      <input className="form-control mb-3" id="record-semester" type="number" min="1" max="12" value={recordForm.semester} onChange={(event) => setRecordForm({ ...recordForm, semester: event.target.value })} required />
                    </div>
                  </div>

                  <div className="d-flex gap-2">
                    <button className="btn btn-primary" type="submit">{editingRecordId ? 'Save record' : 'Add record'}</button>
                    {editingRecordId && <button className="btn btn-outline-secondary" type="button" onClick={resetRecordForm}>Cancel</button>}
                  </div>
                </form>
              </section>

              <section className="academic-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Academic records</h2>
                {records.length === 0 ? (
                  <p className="text-secondary mb-0">No academic records yet.</p>
                ) : (
                  <div className="table-responsive">
                    <table className="table align-middle mb-0">
                      <thead><tr><th>Subject</th><th>Score</th><th>Sem.</th><th aria-label="Actions" /></tr></thead>
                      <tbody>
                        {records.map((record) => (
                          <tr key={record.id}>
                            <td>{record.subject}</td>
                            <td>{record.score}%</td>
                            <td>{record.semester}</td>
                            <td className="text-end text-nowrap">
                              <button className="btn btn-sm btn-outline-primary me-2" type="button" onClick={() => editRecord(record)}>Edit</button>
                              <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => removeRecord(record.id)}>Delete</button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </div>

            <div className="col-lg-6">
              <section className="academic-panel rounded-4 shadow-sm p-4 mb-4">
                <h2 className="h5 mb-3">{editingPlanId ? 'Edit study plan' : 'Add study plan'}</h2>
                <form onSubmit={savePlan}>
                  <label className="form-label" htmlFor="plan-subject">Subject</label>
                  <input className="form-control mb-3" id="plan-subject" value={planForm.subject} onChange={(event) => setPlanForm({ ...planForm, subject: event.target.value })} maxLength="100" required />

                  <div className="row">
                    <div className="col-sm-6">
                      <label className="form-label" htmlFor="target-hours">Target hours</label>
                      <input className="form-control mb-3" id="target-hours" type="number" min="0.1" step="0.1" value={planForm.targetHours} onChange={(event) => setPlanForm({ ...planForm, targetHours: event.target.value })} required />
                    </div>
                    <div className="col-sm-6">
                      <label className="form-label" htmlFor="completed-hours">Completed hours</label>
                      <input className="form-control mb-3" id="completed-hours" type="number" min="0" max={planForm.targetHours || undefined} step="0.1" value={planForm.completedHours} onChange={(event) => setPlanForm({ ...planForm, completedHours: event.target.value })} required />
                    </div>
                  </div>

                  <div className="d-flex gap-2">
                    <button className="btn btn-primary" type="submit">{editingPlanId ? 'Save plan' : 'Add plan'}</button>
                    {editingPlanId && <button className="btn btn-outline-secondary" type="button" onClick={resetPlanForm}>Cancel</button>}
                  </div>
                </form>
              </section>

              <section className="academic-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Study plans</h2>
                {plans.length === 0 ? (
                  <p className="text-secondary mb-0">No study plans yet.</p>
                ) : plans.map((plan) => (
                  <article className="border rounded-3 p-3 mb-3" key={plan.id}>
                    <div className="d-flex justify-content-between gap-3 mb-2">
                      <strong>{plan.subject}</strong>
                      <span>{plan.progress.toFixed(1)}%</span>
                    </div>
                    <div className="progress mb-3" role="progressbar" aria-valuenow={plan.progress} aria-valuemin="0" aria-valuemax="100">
                      <div className="progress-bar" style={{ width: `${plan.progress}%` }} />
                    </div>
                    <div className="input-group input-group-sm mb-3">
                      <input className="form-control" type="number" min="0" max={plan.targetHours} step="0.1" aria-label={`Completed hours for ${plan.subject}`} value={progressValues[plan.id] ?? plan.completedHours} onChange={(event) => setProgressValues({ ...progressValues, [plan.id]: event.target.value })} />
                      <span className="input-group-text">of {plan.targetHours} hours</span>
                      <button className="btn btn-outline-primary" type="button" onClick={() => updateProgress(plan)}>Update</button>
                    </div>
                    <div className="d-flex gap-2">
                      <button className="btn btn-sm btn-outline-primary" type="button" onClick={() => editPlan(plan)}>Edit plan</button>
                      <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => removePlan(plan.id)}>Delete</button>
                    </div>
                  </article>
                ))}
              </section>
            </div>
          </div>
        )}
      </main>
    </>
  )
}
