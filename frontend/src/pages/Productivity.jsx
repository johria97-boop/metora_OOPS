import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../api.js'
import AppNav from '../components/AppNav.jsx'

const emptyTask = { title: '', dueDate: '', priority: 'MEDIUM' }
const emptyHabit = { name: '', targetStreak: '' }
const emptyEvent = { title: '', eventDate: '' }

export default function ProductivityPage() {
  const [activeTab, setActiveTab] = useState('tasks')
  const [tasks, setTasks] = useState([])
  const [habits, setHabits] = useState([])
  const [events, setEvents] = useState([])
  const [taskForm, setTaskForm] = useState(emptyTask)
  const [habitForm, setHabitForm] = useState(emptyHabit)
  const [eventForm, setEventForm] = useState(emptyEvent)
  const [editingTaskId, setEditingTaskId] = useState(null)
  const [editingHabitId, setEditingHabitId] = useState(null)
  const [editingEventId, setEditingEventId] = useState(null)
  const [taskDates, setTaskDates] = useState({})
  const [eventDates, setEventDates] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const today = new Date().toISOString().slice(0, 10)

  useEffect(() => {
    let active = true
    Promise.all([
      api.get('/productivity/tasks'),
      api.get('/productivity/habits'),
      api.get('/productivity/calendar'),
    ])
      .then(([taskResponse, habitResponse, eventResponse]) => {
        if (!active) return
        setTasks(taskResponse.data)
        setHabits(habitResponse.data)
        setEvents(eventResponse.data)
        setTaskDates(Object.fromEntries(taskResponse.data.map((task) => [task.id, task.dueDate])))
        setEventDates(Object.fromEntries(eventResponse.data.map((item) => [item.id, item.eventDate])))
      })
      .catch((requestError) => {
        if (active) handleRequestError(requestError, 'Unable to load productivity data.')
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

  function replaceById(setter, saved) {
    setter((current) => current.map((item) => item.id === saved.id ? saved : item))
  }

  function resetTaskForm() {
    setTaskForm(emptyTask)
    setEditingTaskId(null)
  }

  async function saveTask(event) {
    event.preventDefault()
    beginAction()
    try {
      if (editingTaskId) {
        const { data } = await api.put(`/productivity/tasks/${editingTaskId}`, {
          title: taskForm.title,
          priority: taskForm.priority,
        })
        replaceById(setTasks, data)
        setMessage('Task updated.')
      } else {
        const { data } = await api.post('/productivity/tasks', taskForm)
        setTasks((current) => [...current, data])
        setTaskDates((current) => ({ ...current, [data.id]: data.dueDate }))
        setMessage('Task created.')
      }
      resetTaskForm()
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the task.')
    }
  }

  function editTask(task) {
    setTaskForm({ title: task.title, dueDate: task.dueDate, priority: task.priority })
    setEditingTaskId(task.id)
    beginAction()
  }

  async function completeTask(id) {
    beginAction()
    try {
      const { data } = await api.patch(`/productivity/tasks/${id}/complete`)
      replaceById(setTasks, data)
      setMessage('Task completed.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to complete the task.')
    }
  }

  async function rescheduleTask(id) {
    beginAction()
    try {
      const { data } = await api.patch(`/productivity/tasks/${id}/reschedule`, {
        scheduledDate: taskDates[id],
      })
      replaceById(setTasks, data)
      setMessage('Task rescheduled.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to reschedule the task.')
    }
  }

  async function deleteTask(id) {
    if (!window.confirm('Delete this task?')) return
    beginAction()
    try {
      await api.delete(`/productivity/tasks/${id}`)
      setTasks((current) => current.filter((task) => task.id !== id))
      if (editingTaskId === id) resetTaskForm()
      setMessage('Task deleted.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to delete the task.')
    }
  }

  function resetHabitForm() {
    setHabitForm(emptyHabit)
    setEditingHabitId(null)
  }

  async function saveHabit(event) {
    event.preventDefault()
    beginAction()
    const payload = { name: habitForm.name, targetStreak: Number(habitForm.targetStreak) }
    try {
      if (editingHabitId) {
        const { data } = await api.put(`/productivity/habits/${editingHabitId}`, payload)
        replaceById(setHabits, data)
        setMessage('Habit updated.')
      } else {
        const { data } = await api.post('/productivity/habits', payload)
        setHabits((current) => [...current, data])
        setMessage('Habit created.')
      }
      resetHabitForm()
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the habit.')
    }
  }

  function editHabit(habit) {
    setHabitForm({ name: habit.name, targetStreak: habit.targetStreak })
    setEditingHabitId(habit.id)
    beginAction()
  }

  async function completeHabit(id) {
    beginAction()
    try {
      const { data } = await api.patch(`/productivity/habits/${id}/complete`)
      replaceById(setHabits, data)
      setMessage('Habit completion recorded.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to record habit completion.')
    }
  }

  async function deleteHabit(id) {
    if (!window.confirm('Delete this habit?')) return
    beginAction()
    try {
      await api.delete(`/productivity/habits/${id}`)
      setHabits((current) => current.filter((habit) => habit.id !== id))
      if (editingHabitId === id) resetHabitForm()
      setMessage('Habit deleted.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to delete the habit.')
    }
  }

  function resetEventForm() {
    setEventForm(emptyEvent)
    setEditingEventId(null)
  }

  async function saveEvent(event) {
    event.preventDefault()
    beginAction()
    try {
      if (editingEventId) {
        const { data } = await api.put(`/productivity/calendar/${editingEventId}`, {
          title: eventForm.title,
        })
        replaceById(setEvents, data)
        setMessage('Calendar event updated.')
      } else {
        const { data } = await api.post('/productivity/calendar', eventForm)
        setEvents((current) => [...current, data])
        setEventDates((current) => ({ ...current, [data.id]: data.eventDate }))
        setMessage('Calendar event created.')
      }
      resetEventForm()
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to save the calendar event.')
    }
  }

  function editEvent(item) {
    setEventForm({ title: item.title, eventDate: item.eventDate })
    setEditingEventId(item.id)
    beginAction()
  }

  async function rescheduleEvent(id) {
    beginAction()
    try {
      const { data } = await api.patch(`/productivity/calendar/${id}/reschedule`, {
        scheduledDate: eventDates[id],
      })
      replaceById(setEvents, data)
      setMessage('Calendar event rescheduled.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to reschedule the calendar event.')
    }
  }

  async function deleteEvent(id) {
    if (!window.confirm('Delete this calendar event?')) return
    beginAction()
    try {
      await api.delete(`/productivity/calendar/${id}`)
      setEvents((current) => current.filter((item) => item.id !== id))
      if (editingEventId === id) resetEventForm()
      setMessage('Calendar event deleted.')
    } catch (requestError) {
      handleRequestError(requestError, 'Unable to delete the calendar event.')
    }
  }

  return (
    <>
      <AppNav />
      <main className="container py-4 py-md-5">
        <header className="mb-4">
          <p className="text-uppercase fw-semibold text-primary small mb-1">Productivity module</p>
          <h1 className="h2">Plan work and build consistency</h1>
          <p className="text-secondary mb-0">Manage tasks, habit streaks, and dated events in one place.</p>
        </header>

        {message && <div className="alert alert-success py-2">{message}</div>}
        {error && <div className="alert alert-danger py-2" role="alert">{error}</div>}

        <ul className="nav nav-tabs mb-4" aria-label="Productivity sections">
          {['tasks', 'habits', 'calendar'].map((tab) => (
            <li className="nav-item" key={tab}>
              <button className={`nav-link text-capitalize ${activeTab === tab ? 'active' : ''}`} type="button" onClick={() => setActiveTab(tab)}>{tab}</button>
            </li>
          ))}
        </ul>

        {loading && <div className="text-center py-5">Loading productivity...</div>}

        {!loading && activeTab === 'tasks' && (
          <div className="row g-4">
            <div className="col-lg-4">
              <section className="productivity-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">{editingTaskId ? 'Edit task' : 'Create task'}</h2>
                <form onSubmit={saveTask}>
                  <label className="form-label" htmlFor="task-title">Title</label>
                  <input className="form-control mb-3" id="task-title" value={taskForm.title} onChange={(e) => setTaskForm({ ...taskForm, title: e.target.value })} maxLength="150" required />
                  {!editingTaskId && <><label className="form-label" htmlFor="task-date">Due date</label><input className="form-control mb-3" id="task-date" type="date" min={today} value={taskForm.dueDate} onChange={(e) => setTaskForm({ ...taskForm, dueDate: e.target.value })} required /></>}
                  <label className="form-label" htmlFor="task-priority">Priority</label>
                  <select className="form-select mb-3" id="task-priority" value={taskForm.priority} onChange={(e) => setTaskForm({ ...taskForm, priority: e.target.value })}>
                    <option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option>
                  </select>
                  <button className="btn btn-primary me-2" type="submit">{editingTaskId ? 'Save task' : 'Create task'}</button>
                  {editingTaskId && <button className="btn btn-outline-secondary" type="button" onClick={resetTaskForm}>Cancel</button>}
                </form>
              </section>
            </div>
            <div className="col-lg-8">
              <section className="productivity-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Tasks</h2>
                {tasks.length === 0 ? <p className="text-secondary mb-0">No tasks yet.</p> : tasks.map((task) => (
                  <article className={`border rounded-3 p-3 mb-3 ${task.completed ? 'opacity-75' : ''}`} key={task.id}>
                    <div className="d-flex justify-content-between gap-3 mb-2">
                      <strong className={task.completed ? 'text-decoration-line-through' : ''}>{task.title}</strong>
                      <span className={`badge text-bg-${task.priority === 'HIGH' ? 'danger' : task.priority === 'MEDIUM' ? 'warning' : 'secondary'}`}>{task.priority}</span>
                    </div>
                    <div className="input-group input-group-sm mb-3">
                      <input className="form-control" type="date" min={today} aria-label={`Due date for ${task.title}`} value={taskDates[task.id] ?? task.dueDate} onChange={(e) => setTaskDates({ ...taskDates, [task.id]: e.target.value })} />
                      <button className="btn btn-outline-secondary" type="button" onClick={() => rescheduleTask(task.id)}>Reschedule</button>
                    </div>
                    <div className="d-flex flex-wrap gap-2">
                      <button className="btn btn-sm btn-success" type="button" disabled={task.completed} onClick={() => completeTask(task.id)}>{task.completed ? 'Completed' : 'Complete'}</button>
                      <button className="btn btn-sm btn-outline-primary" type="button" onClick={() => editTask(task)}>Edit</button>
                      <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => deleteTask(task.id)}>Delete</button>
                    </div>
                  </article>
                ))}
              </section>
            </div>
          </div>
        )}

        {!loading && activeTab === 'habits' && (
          <div className="row g-4">
            <div className="col-lg-4">
              <section className="productivity-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">{editingHabitId ? 'Edit habit' : 'Create habit'}</h2>
                <form onSubmit={saveHabit}>
                  <label className="form-label" htmlFor="habit-name">Name</label>
                  <input className="form-control mb-3" id="habit-name" value={habitForm.name} onChange={(e) => setHabitForm({ ...habitForm, name: e.target.value })} maxLength="100" required />
                  <label className="form-label" htmlFor="habit-target">Target streak</label>
                  <input className="form-control mb-3" id="habit-target" type="number" min="1" value={habitForm.targetStreak} onChange={(e) => setHabitForm({ ...habitForm, targetStreak: e.target.value })} required />
                  <button className="btn btn-primary me-2" type="submit">{editingHabitId ? 'Save habit' : 'Create habit'}</button>
                  {editingHabitId && <button className="btn btn-outline-secondary" type="button" onClick={resetHabitForm}>Cancel</button>}
                </form>
              </section>
            </div>
            <div className="col-lg-8">
              <section className="productivity-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Habits</h2>
                {habits.length === 0 ? <p className="text-secondary mb-0">No habits yet.</p> : habits.map((habit) => (
                  <article className="border rounded-3 p-3 mb-3" key={habit.id}>
                    <div className="d-flex justify-content-between gap-3"><strong>{habit.name}</strong><span>{habit.currentStreak} / {habit.targetStreak} days</span></div>
                    <div className="progress my-3" role="progressbar" aria-valuenow={habit.progress} aria-valuemin="0" aria-valuemax="100"><div className="progress-bar" style={{ width: `${habit.progress}%` }} /></div>
                    <p className="small text-secondary">Last completed: {habit.lastCompletedDate ?? 'Not yet completed'} · {habit.progress.toFixed(1)}%</p>
                    <div className="d-flex flex-wrap gap-2">
                      <button className="btn btn-sm btn-success" type="button" onClick={() => completeHabit(habit.id)}>Complete today</button>
                      <button className="btn btn-sm btn-outline-primary" type="button" onClick={() => editHabit(habit)}>Edit</button>
                      <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => deleteHabit(habit.id)}>Delete</button>
                    </div>
                  </article>
                ))}
              </section>
            </div>
          </div>
        )}

        {!loading && activeTab === 'calendar' && (
          <div className="row g-4">
            <div className="col-lg-4">
              <section className="productivity-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">{editingEventId ? 'Edit event' : 'Create event'}</h2>
                <form onSubmit={saveEvent}>
                  <label className="form-label" htmlFor="event-title">Title</label>
                  <input className="form-control mb-3" id="event-title" value={eventForm.title} onChange={(e) => setEventForm({ ...eventForm, title: e.target.value })} maxLength="150" required />
                  {!editingEventId && <><label className="form-label" htmlFor="event-date">Date</label><input className="form-control mb-3" id="event-date" type="date" min={today} value={eventForm.eventDate} onChange={(e) => setEventForm({ ...eventForm, eventDate: e.target.value })} required /></>}
                  <button className="btn btn-primary me-2" type="submit">{editingEventId ? 'Save event' : 'Create event'}</button>
                  {editingEventId && <button className="btn btn-outline-secondary" type="button" onClick={resetEventForm}>Cancel</button>}
                </form>
              </section>
            </div>
            <div className="col-lg-8">
              <section className="productivity-panel rounded-4 shadow-sm p-4">
                <h2 className="h5 mb-3">Calendar events</h2>
                {events.length === 0 ? <p className="text-secondary mb-0">No calendar events yet.</p> : events.map((item) => (
                  <article className="border rounded-3 p-3 mb-3" key={item.id}>
                    <strong className="d-block mb-2">{item.title}</strong>
                    <div className="input-group input-group-sm mb-3">
                      <input className="form-control" type="date" min={today} aria-label={`Date for ${item.title}`} value={eventDates[item.id] ?? item.eventDate} onChange={(e) => setEventDates({ ...eventDates, [item.id]: e.target.value })} />
                      <button className="btn btn-outline-secondary" type="button" onClick={() => rescheduleEvent(item.id)}>Reschedule</button>
                    </div>
                    <button className="btn btn-sm btn-outline-primary me-2" type="button" onClick={() => editEvent(item)}>Edit</button>
                    <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => deleteEvent(item.id)}>Delete</button>
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
