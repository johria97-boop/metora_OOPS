import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

const emptyProfile = { careerGoal: '', interests: [] }
const emptySkill = { name: '', level: 'BEGINNER' }

export default function CareerPage() {
  const [profile, setProfile] = useState(emptyProfile)
  const [interestInput, setInterestInput] = useState('')
  const [skills, setSkills] = useState([])
  const [skillForm, setSkillForm] = useState(emptySkill)
  const [editingSkillId, setEditingSkillId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  useEffect(() => {
    let active = true
    Promise.all([api.get('/career'), api.get('/career/skills')])
      .then(([profileResponse, skillResponse]) => {
        if (!active) return
        setProfile({
          careerGoal: profileResponse.data.careerGoal,
          interests: profileResponse.data.interests,
        })
        setSkills(skillResponse.data)
      })
      .catch((requestError) => {
        if (active) handleRequestError(requestError, 'Unable to load career information.')
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

  function addInterest() {
    beginAction()
    const cleanInterest = interestInput.trim()
    if (!cleanInterest) {
      setError('Enter an interest before adding it.')
      return
    }
    if (cleanInterest.length > 80) {
      setError('An interest cannot exceed 80 characters.')
      return
    }
    if (profile.interests.length >= 10) {
      setError('You can add up to 10 interests.')
      return
    }
    if (profile.interests.some((item) => item.toLowerCase() === cleanInterest.toLowerCase())) {
      setError('That interest is already included.')
      return
    }
    setProfile((current) => ({
      ...current,
      interests: [...current.interests, cleanInterest],
    }))
    setInterestInput('')
  }

  function removeInterest(interest) {
    beginAction()
    setProfile((current) => ({
      ...current,
      interests: current.interests.filter((item) => item !== interest),
    }))
  }

  async function saveProfile(event) {
    event.preventDefault()
    beginAction()
    try {
      const { data } = await api.put('/career', profile)
      setProfile({ careerGoal: data.careerGoal, interests: data.interests })
      setMessage('Career profile saved.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the career profile.')
    }
  }

  function resetSkillForm() {
    setSkillForm(emptySkill)
    setEditingSkillId(null)
  }

  async function saveSkill(event) {
    event.preventDefault()
    beginAction()
    try {
      if (editingSkillId) {
        const { data } = await api.put(`/career/skills/${editingSkillId}`, skillForm)
        setSkills((current) => current.map((skill) => skill.id === data.id ? data : skill))
        setMessage('Skill updated.')
      } else {
        const { data } = await api.post('/career/skills', skillForm)
        setSkills((current) => [...current, data])
        setMessage('Skill added.')
      }
      resetSkillForm()
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the skill.')
    }
  }

  function editSkill(skill) {
    beginAction()
    setEditingSkillId(skill.id)
    setSkillForm({ name: skill.name, level: skill.level })
  }

  async function deleteSkill(id) {
    if (!window.confirm('Delete this skill?')) return
    beginAction()
    try {
      await api.delete(`/career/skills/${id}`)
      setSkills((current) => current.filter((skill) => skill.id !== id))
      if (editingSkillId === id) resetSkillForm()
      setMessage('Skill deleted.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to delete the skill.')
    }
  }

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        <header className="mb-4">
          <p className="text-uppercase fw-semibold text-primary small mb-1">Career module</p>
          <h1 className="h2">Career profile and skills</h1>
          <p className="text-secondary mb-0">Capture your direction, interests, and current skill levels.</p>
        </header>

        {message && <div className="alert alert-success py-2">{message}</div>}
        {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}
        {loading && <div className="text-center py-5">Loading career information...</div>}

        {!loading && (
          <div className="row g-4">
            <div className="col-lg-6">
              <section className="career-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Career profile</h2>
                <form onSubmit={saveProfile}>
                  <label className="form-label" htmlFor="career-goal">Career goal</label>
                  <input
                    className="form-control mb-3"
                    id="career-goal"
                    value={profile.careerGoal}
                    onChange={(event) => setProfile({ ...profile, careerGoal: event.target.value })}
                    maxLength="150"
                    placeholder="Example: Backend Developer"
                    required
                  />

                  <label className="form-label" htmlFor="interest">Interests</label>
                  <div className="input-group mb-3">
                    <input
                      className="form-control"
                      id="interest"
                      value={interestInput}
                      onChange={(event) => setInterestInput(event.target.value)}
                      maxLength="80"
                      placeholder="Example: Artificial Intelligence"
                    />
                    <button className="btn btn-outline-primary" type="button" onClick={addInterest}>Add</button>
                  </div>

                  <div className="d-flex flex-wrap gap-2 mb-4">
                    {profile.interests.length === 0 && <span className="text-secondary small">No interests added yet.</span>}
                    {profile.interests.map((interest) => (
                      <span className="badge rounded-pill text-bg-light border career-interest" key={interest.toLowerCase()}>
                        {interest}
                        <button className="btn-close ms-2" type="button" aria-label={`Remove ${interest}`} onClick={() => removeInterest(interest)} />
                      </span>
                    ))}
                  </div>
                  <button className="btn btn-primary" type="submit">Save career profile</button>
                </form>
              </section>
            </div>

            <div className="col-lg-6">
              <section className="career-panel rounded-4 shadow-sm p-4 mb-4">
                <h2 className="h5 mb-3">{editingSkillId ? 'Edit skill' : 'Add skill'}</h2>
                <form onSubmit={saveSkill}>
                  <div className="row g-3">
                    <div className="col-sm-7">
                      <label className="form-label" htmlFor="skill-name">Skill name</label>
                      <input className="form-control" id="skill-name" value={skillForm.name} onChange={(event) => setSkillForm({ ...skillForm, name: event.target.value })} maxLength="100" required />
                    </div>
                    <div className="col-sm-5">
                      <label className="form-label" htmlFor="skill-level">Level</label>
                      <select className="form-select" id="skill-level" value={skillForm.level} onChange={(event) => setSkillForm({ ...skillForm, level: event.target.value })}>
                        <option value="BEGINNER">Beginner</option>
                        <option value="INTERMEDIATE">Intermediate</option>
                        <option value="ADVANCED">Advanced</option>
                      </select>
                    </div>
                  </div>
                  <div className="mt-3">
                    <button className="btn btn-primary me-2" type="submit">{editingSkillId ? 'Save skill' : 'Add skill'}</button>
                    {editingSkillId && <button className="btn btn-outline-secondary" type="button" onClick={resetSkillForm}>Cancel</button>}
                  </div>
                </form>
              </section>

              <section className="career-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Skills</h2>
                {skills.length === 0 ? (
                  <p className="text-secondary mb-0">No skills added yet.</p>
                ) : (
                  <div className="table-responsive">
                    <table className="table align-middle mb-0">
                      <thead><tr><th>Skill</th><th>Level</th><th aria-label="Actions" /></tr></thead>
                      <tbody>
                        {skills.map((skill) => (
                          <tr key={skill.id}>
                            <td>{skill.name}</td>
                            <td><span className="badge text-bg-primary">{skill.level}</span></td>
                            <td className="text-end text-nowrap">
                              <button className="btn btn-sm btn-outline-primary me-2" type="button" onClick={() => editSkill(skill)}>Edit</button>
                              <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => deleteSkill(skill.id)}>Delete</button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </div>
          </div>
        )}
      </main>
    </>
  )
}
