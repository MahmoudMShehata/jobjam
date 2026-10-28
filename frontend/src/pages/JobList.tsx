import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type Job } from '../api'

function hostname(url: string) {
  try {
    return new URL(url).hostname
  } catch {
    return url
  }
}

export default function JobList() {
  const [jobs, setJobs] = useState<Job[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.listJobs().then(setJobs).catch((e: Error) => setError(e.message))
  }, [])

  return (
    <>
      <div className="page-header">
        <h1>My jobs</h1>
        <Link to="/jobs/new" className="button">+ Add job</Link>
      </div>

      {error && <p className="error">Could not load jobs: {error}</p>}
      {!jobs && !error && <p className="muted">Loading…</p>}

      {jobs?.length === 0 && (
        <div className="empty">
          <p>No jobs yet.</p>
          <Link to="/jobs/new" className="button">Add your first job</Link>
        </div>
      )}

      <ul className="job-list">
        {jobs?.map((job) => (
          <li key={job.id}>
            <Link to={`/jobs/${job.id}`} className="job-card">
              <h2>{job.title}</h2>
              {job.description && <p className="snippet">{job.description}</p>}
              <span className="muted">
                Added {new Date(job.createdAt).toLocaleDateString()}
                {job.url && ` · ${hostname(job.url)}`}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </>
  )
}
