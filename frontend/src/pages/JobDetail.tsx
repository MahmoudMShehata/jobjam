import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, type Job } from '../api'

export default function JobDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [job, setJob] = useState<Job | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getJob(Number(id)).then(setJob).catch((e: Error) => setError(e.message))
  }, [id])

  async function handleDelete() {
    if (!job || !confirm(`Delete "${job.title}"?`)) return
    try {
      await api.deleteJob(job.id)
      navigate('/')
    } catch (err) {
      setError((err as Error).message)
    }
  }

  if (error) return <p className="error">Could not load job: {error}</p>
  if (!job) return <p className="muted">Loading…</p>

  const sections: [string, string | null][] = [
    ['Description', job.description],
    ['Requirements', job.requirements],
    ['Benefits', job.benefits],
  ]

  return (
    <>
      <Link to="/" className="back">← All jobs</Link>
      <div className="page-header">
        <h1>{job.title}</h1>
        <button className="danger" onClick={handleDelete}>Delete</button>
      </div>
      {sections.map(([heading, text]) => (
        <section key={heading} className="detail-section">
          <h2>{heading}</h2>
          {text ? <p className="prewrap">{text}</p> : <p className="muted">Not provided</p>}
        </section>
      ))}
    </>
  )
}
