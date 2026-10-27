export interface Job {
  id: number
  title: string
  description: string | null
  requirements: string | null
  benefits: string | null
  createdAt: string
}

export type JobInput = Pick<Job, 'title' | 'description' | 'requirements' | 'benefits'>

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) {
    throw new Error(`Request failed (${res.status})`)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

export const api = {
  listJobs: () => request<Job[]>('/jobs'),
  getJob: (id: number) => request<Job>(`/jobs/${id}`),
  createJob: (job: JobInput) =>
    request<Job>('/jobs', { method: 'POST', body: JSON.stringify(job) }),
  deleteJob: (id: number) => request<void>(`/jobs/${id}`, { method: 'DELETE' }),
}
