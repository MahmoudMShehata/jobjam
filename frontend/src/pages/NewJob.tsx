import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, type JobInput } from '../api'

const emptyJob: JobInput = { title: '', description: '', requirements: '', benefits: '' }

export default function NewJob() {
  const navigate = useNavigate()
  const [job, setJob] = useState<JobInput>(emptyJob)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const update = (field: keyof JobInput) =>
    (e: { target: { value: string } }) => setJob({ ...job, [field]: e.target.value })

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await api.createJob(job)
      navigate('/')
    } catch (err) {
      setError((err as Error).message)
      setSaving(false)
    }
  }

  return (
    <>
      <div className="page-header">
        <h1>Add a job</h1>
      </div>

      <form className="job-form" onSubmit={handleSubmit}>
        <label>
          Job title
          <input value={job.title ?? ''} onChange={update('title')} required maxLength={255} autoFocus />
        </label>
        <label>
          Description
          <textarea value={job.description ?? ''} onChange={update('description')} rows={6} />
        </label>
        <label>
          Requirements
          <textarea value={job.requirements ?? ''} onChange={update('requirements')} rows={6} />
        </label>
        <label>
          Benefits
          <textarea value={job.benefits ?? ''} onChange={update('benefits')} rows={4} />
        </label>

        {error && <p className="error">Could not save job: {error}</p>}

        <div className="actions">
          <Link to="/" className="button secondary">Cancel</Link>
          <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save job'}</button>
        </div>
      </form>
    </>
  )
}
