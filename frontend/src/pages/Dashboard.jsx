import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

const priorityStyles = {
  URGENT: 'text-bg-danger',
  HIGH: 'text-bg-warning',
  MEDIUM: 'text-bg-primary',
  LOW: 'text-bg-secondary',
}

function percent(value) {
  return value == null ? '—' : `${value.toFixed(1)}%`
}

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    api.get('/dashboard')
      .then(({ data }) => {
        if (active) setDashboard(data)
      })
      .catch((requestError) => {
        if (!active) return
        setError(getApiErrorMessage(requestError, 'Unable to load your dashboard.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        {loading && <div className="text-center py-5">Loading your dashboard...</div>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}

        {!loading && dashboard && (
          <>
            <header className="dashboard-welcome rounded-4 shadow-sm p-4 mb-4">
              <p className="text-uppercase fw-semibold small mb-1">Student dashboard</p>
              <h1 className="h2 mb-2">Welcome, {dashboard.student.name}</h1>
              <p className="mb-0 opacity-75">{dashboard.student.branch} · Semester {dashboard.student.semester}</p>
            </header>

            <section className="row g-3 mb-4" aria-label="Quick summary">
              <SummaryMetric label="Academic average" value={percent(dashboard.academics.averageScore)} />
              <SummaryMetric label="Pending tasks" value={dashboard.tasks.pendingTaskCount} />
              <SummaryMetric label="Active habits" value={dashboard.habits.totalHabits} />
              <SummaryMetric label="Active recommendations" value={dashboard.recommendations.activeCount} />
              <SummaryMetric label="Active roadmaps" value={dashboard.roadmaps.activeRoadmapCount} />
            </section>

            <section className="dashboard-panel rounded-4 shadow-sm p-4 mb-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Learning roadmap</h2>
                <Link to="/roadmaps">View roadmaps</Link>
              </div>
              {dashboard.roadmaps.currentRoadmap == null ? (
                <p className="text-secondary mb-0">No saved roadmap yet — <Link to="/roadmaps">generate a learning path</Link>.</p>
              ) : (
                <>
                  <div className="d-flex justify-content-between small mb-1">
                    <strong>{dashboard.roadmaps.currentRoadmap.courseName}</strong>
                    <span>{dashboard.roadmaps.currentRoadmap.completedSteps}/{dashboard.roadmaps.currentRoadmap.totalSteps} steps · {percent(dashboard.roadmaps.currentRoadmap.progress)}</span>
                  </div>
                  <div className="progress" role="progressbar" aria-label={`${dashboard.roadmaps.currentRoadmap.courseName} progress`} aria-valuenow={dashboard.roadmaps.currentRoadmap.progress} aria-valuemin="0" aria-valuemax="100">
                    <div className="progress-bar bg-success" style={{ width: `${dashboard.roadmaps.currentRoadmap.progress}%` }} />
                  </div>
                </>
              )}
            </section>

            <div className="row g-4 mb-4">
              <div className="col-lg-6">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2 className="h5 mb-0">Academic overview</h2>
                    <Link to="/academics">View academics</Link>
                  </div>
                  {dashboard.academics.subjectCount === 0 ? (
                    <p className="text-secondary mb-0">No academic records yet — <Link to="/academics">add academics</Link>.</p>
                  ) : (
                    <>
                      <div className="row g-3 mb-3">
                        <div className="col-sm-6"><Fact label="Strongest subject" value={`${dashboard.academics.strongestSubject.subject} (${percent(dashboard.academics.strongestSubject.score)})`} /></div>
                        <div className="col-sm-6"><Fact label="Attention subject" value={`${dashboard.academics.attentionSubject.subject} (${percent(dashboard.academics.attentionSubject.score)})`} /></div>
                      </div>
                      <ul className="list-group list-group-flush">
                        {dashboard.academics.subjects.map((item, index) => (
                          <li className="list-group-item px-0 d-flex justify-content-between" key={`${item.semester}-${item.subject}-${index}`}>
                            <span>{item.subject} <small className="text-secondary">Sem {item.semester}</small></span>
                            <strong>{percent(item.score)}</strong>
                          </li>
                        ))}
                      </ul>
                    </>
                  )}
                </section>
              </div>

              <div className="col-lg-6">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2 className="h5 mb-0">Study plans</h2>
                    <span className="text-secondary small">Average {percent(dashboard.studyPlans.averageProgress)}</span>
                  </div>
                  {dashboard.studyPlans.totalPlans === 0 ? (
                    <p className="text-secondary mb-0">No study plans yet — <Link to="/academics">create a plan</Link>.</p>
                  ) : dashboard.studyPlans.plans.map((plan, index) => (
                    <div className="mb-3" key={`${plan.subject}-${index}`}>
                      <div className="d-flex justify-content-between small mb-1"><span>{plan.subject}</span><span>{percent(plan.progress)}</span></div>
                      <div className="progress" role="progressbar" aria-label={`${plan.subject} progress`} aria-valuenow={plan.progress} aria-valuemin="0" aria-valuemax="100">
                        <div className="progress-bar" style={{ width: `${plan.progress}%` }} />
                      </div>
                    </div>
                  ))}
                </section>
              </div>
            </div>

            <div className="row g-4 mb-4">
              <div className="col-lg-4">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <h2 className="h5">Next tasks</h2>
                  <p className="small text-secondary">{dashboard.tasks.highPriorityPendingCount} high priority</p>
                  {dashboard.tasks.nextDueTasks.length === 0 ? <Empty text="No pending tasks." link="/productivity" action="Add a task" /> : (
                    <ul className="list-group list-group-flush">
                      {dashboard.tasks.nextDueTasks.map((task, index) => (
                        <li className="list-group-item px-0" key={`${task.dueDate}-${task.title}-${index}`}>
                          <strong className="d-block">{task.title}</strong>
                          <small className="text-secondary">Due {task.dueDate} · {task.priority}</small>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              </div>

              <div className="col-lg-4">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <h2 className="h5">Habits</h2>
                  {dashboard.habits.habits.length === 0 ? <Empty text="No habits yet." link="/productivity" action="Create a habit" /> : dashboard.habits.habits.map((habit, index) => (
                    <div className="mb-3" key={`${habit.name}-${index}`}>
                      <div className="d-flex justify-content-between small"><strong>{habit.name}</strong><span>{habit.currentStreak}/{habit.targetStreak} days</span></div>
                      <div className="progress mt-2" role="progressbar" aria-label={`${habit.name} progress`} aria-valuenow={habit.progress} aria-valuemin="0" aria-valuemax="100">
                        <div className="progress-bar bg-success" style={{ width: `${habit.progress}%` }} />
                      </div>
                    </div>
                  ))}
                </section>
              </div>

              <div className="col-lg-4">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <h2 className="h5">Upcoming events</h2>
                  {dashboard.calendar.upcomingEvents.length === 0 ? <Empty text="No upcoming events." link="/productivity" action="Add an event" /> : (
                    <ul className="list-group list-group-flush">
                      {dashboard.calendar.upcomingEvents.map((item, index) => (
                        <li className="list-group-item px-0" key={`${item.eventDate}-${item.title}-${index}`}>
                          <strong className="d-block">{item.title}</strong>
                          <small className="text-secondary">{item.eventDate}</small>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              </div>
            </div>

            <div className="row g-4">
              <div className="col-lg-5">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <div className="d-flex justify-content-between align-items-center mb-3"><h2 className="h5 mb-0">Career direction</h2><Link to="/career">View career</Link></div>
                  {dashboard.career.setupNeeded ? (
                    <p className="text-secondary mb-0">No career goal yet — <Link to="/career">complete your career profile</Link>.</p>
                  ) : (
                    <>
                      <Fact label="Career goal" value={dashboard.career.careerGoal} />
                      <div className="d-flex flex-wrap gap-2 my-3">{dashboard.career.interests.map((interest) => <span className="badge rounded-pill text-bg-light border" key={interest}>{interest}</span>)}</div>
                      <p className="small text-secondary mb-2">{dashboard.career.skillCount} saved skills</p>
                      <ul className="list-group list-group-flush">{dashboard.career.skills.map((skill) => <li className="list-group-item px-0 d-flex justify-content-between" key={skill.name}><span>{skill.name}</span><span className="badge text-bg-primary">{skill.level}</span></li>)}</ul>
                    </>
                  )}
                </section>
              </div>

              <div className="col-lg-7">
                <section className="dashboard-panel rounded-4 shadow-sm p-4 h-100">
                  <div className="d-flex justify-content-between align-items-center mb-3"><h2 className="h5 mb-0">Active recommendations</h2><Link to="/recommendations">View all</Link></div>
                  {dashboard.recommendations.activeRecommendations.length === 0 ? (
                    <p className="text-secondary mb-0">No active recommendations — <Link to="/recommendations">generate recommendations</Link>.</p>
                  ) : dashboard.recommendations.activeRecommendations.map((item) => (
                    <article className="border rounded-3 p-3 mb-3" key={`${item.category}-${item.title}`}>
                      <div className="d-flex gap-2 mb-2"><span className="badge text-bg-light border">{item.category}</span><span className={`badge ${priorityStyles[item.priority]}`}>{item.priority}</span></div>
                      <h3 className="h6">{item.title}</h3>
                      <p className="small text-secondary mb-0">{item.reason}</p>
                    </article>
                  ))}
                </section>
              </div>
            </div>
          </>
        )}
      </main>
    </>
  )
}

function SummaryMetric({ label, value }) {
  return <div className="col-6 col-md-4 col-xl"><article className="dashboard-metric rounded-4 shadow-sm p-3 h-100"><p className="small text-secondary mb-1">{label}</p><strong className="h4 mb-0">{value}</strong></article></div>
}

function Fact({ label, value }) {
  return <div><p className="small text-secondary mb-1">{label}</p><strong>{value}</strong></div>
}

function Empty({ text, link, action }) {
  return <p className="text-secondary mb-0">{text} <Link to={link}>{action}</Link>.</p>
}
