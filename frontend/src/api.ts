export interface Job {
  id: number
  title: string
  description: string | null
  requirements: string | null
  benefits: string | null
  url: string | null
  details: string | null
  createdAt: string
}

export type JobInput = Pick<Job, 'title' | 'description' | 'requirements' | 'benefits' | 'url' | 'details'>

export interface JobDraft {
  title: string
  details: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  // FormData bodies need the browser to set their own multipart Content-Type.
  const isJson = typeof init?.body === 'string'
  const res = await fetch(`/api${path}`, {
    headers: isJson ? { 'Content-Type': 'application/json' } : undefined,
    ...init,
  })
  if (!res.ok) {
    // Spring Boot error bodies carry a human-readable "message".
    const body = await res.json().catch(() => null)
    throw new Error(body?.message || `Request failed (${res.status})`)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

export const api = {
  listJobs: () => request<Job[]>('/jobs'),
  getJob: (id: number) => request<Job>(`/jobs/${id}`),
  createJob: (job: JobInput) =>
    request<Job>('/jobs', { method: 'POST', body: JSON.stringify(job) }),
  deleteJob: (id: number) => request<void>(`/jobs/${id}`, { method: 'DELETE' }),
  extractJob: (url: string, postingText?: string) =>
    request<JobDraft>('/jobs/extract', { method: 'POST', body: JSON.stringify({ url, postingText }) }),
  extractFromScreenshots: (files: File[], url: string) => {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    if (url) form.append('url', url)
    return request<JobDraft>('/jobs/extract/screenshots', { method: 'POST', body: form })
  },
}
