import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

const priorityStyles = {
  URGENT: 'text-bg-danger',
  HIGH: 'text-bg-warning',
  MEDIUM: 'text-bg-primary',
  LOW: 'text-bg-secondary',
}

export default function RecommendationsPage() {
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  useEffect(() => {
    let active = true
    api.get('/recommendations')
      .then(({ data }) => {
        if (active) setRecommendations(data)
      })
      .catch((requestError) => {
        if (active) handleRequestError(requestError, 'Unable to load recommendations.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  function handleRequestError(requestError, fallback) {
    setError(getApiErrorMessage(requestError, fallback))
  }

  function beginAction() {
    setError('')
    setMessage('')
  }

  async function generateRecommendations() {
    beginAction()
    setGenerating(true)
    try {
      const { data } = await api.post('/recommendations/generate')
      setRecommendations((current) => {
        const generatedIds = new Set(data.map((item) => item.id))
        return [...data, ...current.filter((item) => !generatedIds.has(item.id))]
      })
      setMessage(data.length === 0
        ? 'No valid new recommendations were generated.'
        : `${data.length} recommendation${data.length === 1 ? '' : 's'} generated.`)
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to generate recommendations.')
    } finally {
      setGenerating(false)
    }
  }

  async function updateStatus(id, action) {
    beginAction()
    try {
      const { data } = await api.patch(`/recommendations/${id}/${action}`)
      setRecommendations((current) => current.map((item) => item.id === data.id ? data : item))
      setMessage(action === 'complete' ? 'Recommendation completed.' : 'Recommendation dismissed.')
    } catch (requestError) {
      handleRequestError(requestError, `Unable to ${action} the recommendation.`)
    }
  }

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        <header className="d-sm-flex justify-content-between align-items-start gap-3 mb-4">
          <div>
            <p className="text-uppercase fw-semibold text-primary small mb-1">Personalized mentoring</p>
            <h1 className="h2">Recommendations</h1>
            <p className="text-secondary mb-0">Generate focused next steps from your saved Mentora data.</p>
          </div>
          <button className="btn btn-primary mt-3 mt-sm-0" type="button" disabled={loading || generating} onClick={generateRecommendations}>
            {generating ? 'Generating...' : 'Generate recommendations'}
          </button>
        </header>

        {message && <div className="alert alert-success py-2">{message}</div>}
        {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}
        {loading && <div className="text-center py-5">Loading recommendations...</div>}

        {!loading && recommendations.length === 0 && (
          <section className="recommendation-panel rounded-4 shadow-sm p-4 text-center">
            <h2 className="h5">No recommendations yet</h2>
            <p className="text-secondary mb-0">Add academic, productivity, or career information, then generate personalized guidance.</p>
          </section>
        )}

        {!loading && recommendations.length > 0 && (
          <section className="recommendation-panel rounded-4 shadow-sm p-4">
            <div className="row g-3">
              {recommendations.map((item) => (
                <div className="col-lg-6" key={item.id}>
                  <article className={`recommendation-card border rounded-3 p-3 h-100 ${item.status !== 'ACTIVE' ? 'recommendation-closed' : ''}`}>
                    <div className="d-flex flex-wrap gap-2 mb-2">
                      <span className="badge text-bg-light border">{item.category}</span>
                      <span className={`badge ${priorityStyles[item.priority] ?? 'text-bg-secondary'}`}>{item.priority}</span>
                      <span className="badge text-bg-light border">{item.status}</span>
                    </div>
                    <h2 className="h5">{item.title}</h2>
                    <p className="text-secondary">{item.reason}</p>
                    <p className="small text-secondary mb-3">Created {new Date(item.createdAt).toLocaleString()}</p>
                    {item.status === 'ACTIVE' && (
                      <div className="d-flex gap-2">
                        <button className="btn btn-sm btn-success" type="button" onClick={() => updateStatus(item.id, 'complete')}>Complete</button>
                        <button className="btn btn-sm btn-outline-secondary" type="button" onClick={() => updateStatus(item.id, 'dismiss')}>Dismiss</button>
                      </div>
                    )}
                  </article>
                </div>
              ))}
            </div>
          </section>
        )}
      </main>
    </>
  )
}
