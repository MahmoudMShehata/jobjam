import { Link, Route, Routes } from 'react-router-dom'
import JobList from './pages/JobList'
import NewJob from './pages/NewJob'
import JobDetail from './pages/JobDetail'

export default function App() {
  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">JobJam</Link>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<JobList />} />
          <Route path="/jobs/new" element={<NewJob />} />
          <Route path="/jobs/:id" element={<JobDetail />} />
        </Routes>
      </main>
    </>
  )
}
