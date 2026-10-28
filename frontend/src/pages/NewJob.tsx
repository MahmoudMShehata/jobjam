import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, type JobInput } from '../api'
import ScreenshotPicker from './ScreenshotPicker'

type Source = 'link' | 'text' | 'screenshots'

const SOURCES: [Source, string][] = [
  ['link', 'Link'],
  ['text', 'Pasted text'],
  ['screenshots', 'Screenshots'],
]

const emptyJob: JobInput = {
  title: '', description: '', requirements: '', benefits: '', url: '', details: '',
}

export default function NewJob() {
  const navigate = useNavigate()
  const [job, setJob] = useState<JobInput>(emptyJob)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [source, setSource] = useState<Source>('link')
  const [postingText, setPostingText] = useState('')
  const [screenshots, setScreenshots] = useState<File[]>([])
  const [extracting, setExtracting] = useState(false)
  const [extractError, setExtractError] = useState<string | null>(null)

  const update = (field: keyof JobInput) =>
    (e: { target: { value: string } }) => setJob({ ...job, [field]: e.target.value })

  const hasInput = {
    link: Boolean(job.url?.trim()),
    text: Boolean(postingText.trim()),
    screenshots: screenshots.length > 0,
  }[source]
  const canExtract = !extracting && hasInput

  async function handleExtract() {
    setExtracting(true)
    setExtractError(null)
    try {
      const url = job.url?.trim() ?? ''
      const draft = source === 'screenshots'
        ? await api.extractFromScreenshots(screenshots, url)
        : await api.extractJob(url, source === 'text' ? postingText : undefined)
      setJob((current) => ({
        ...current,
        title: current.title?.trim() ? current.title : draft.title,
        details: draft.details,
      }))
    } catch (err) {
      setExtractError((err as Error).message)
    } finally {
      setExtracting(false)
    }
  }

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
        <div className="extract-box">
          <label>
            Job link
            <input
              type="url"
              value={job.url ?? ''}
              onChange={update('url')}
              placeholder="https://…"
              autoFocus
            />
          </label>

          <div className="source-tabs" role="tablist" aria-label="Extract from">
            {SOURCES.map(([value, label]) => (
              <button
                key={value}
                type="button"
                role="tab"
                aria-selected={source === value}
                className={source === value ? 'active' : ''}
                onClick={() => { setSource(value); setExtractError(null) }}
              >
                {label}
              </button>
            ))}
          </div>

          {source === 'text' && (
            <label>
              Posting text
              <textarea
                value={postingText}
                onChange={(e) => setPostingText(e.target.value)}
                rows={8}
                placeholder="Paste the full job posting here"
              />
            </label>
          )}
          {source === 'screenshots' && <ScreenshotPicker files={screenshots} onChange={setScreenshots} />}

          <div className="extract-actions">
            <button type="button" onClick={handleExtract} disabled={!canExtract}>
              {extracting ? 'Extracting…' : 'Extract with AI'}
            </button>
          </div>
          {extracting && <p className="muted">Claude is reading the posting, this can take a little while…</p>}
          {extractError && (
            <p className="error">
              {extractError}
              {source === 'link' && ' If the page is unreachable, use pasted text or screenshots instead.'}
            </p>
          )}
        </div>

        <label>
          Job title
          <input value={job.title ?? ''} onChange={update('title')} required maxLength={255} />
        </label>
        <label>
          Details
          <textarea
            value={job.details ?? ''}
            onChange={update('details')}
            rows={14}
            placeholder="Filled in by “Extract with AI”, or write your own (Markdown supported)"
          />
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
          <button type="submit" disabled={saving || extracting}>{saving ? 'Saving…' : 'Save job'}</button>
        </div>
      </form>
    </>
  )
}
