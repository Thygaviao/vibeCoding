import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import { Submission, SubmissionStatus } from '../../types'

export function SubmissionDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [item, setItem] = useState<Submission | null>(null)

  useEffect(() => {
    api.get('/api/admin/auth/me').catch(() => navigate('/admin/login'))
    api.get(`/api/admin/submissions/${id}`).then((r) => setItem(r.data))
  }, [id, navigate])

  if (!item) return <section className="card">Loading...</section>

  const updateStatus = async (status: SubmissionStatus) => {
    await api.patch(`/api/admin/submissions/${item.id}/status`, { status })
    setItem({ ...item, status })
  }

  return (
    <section className="card">
      <Link to="/admin/submissions">← Back</Link>
      <h2>Submission #{item.id}</h2>
      <p><b>Student:</b> {item.studentName}</p>
      <p><b>Class:</b> {item.classGroup}</p>
      <p><b>Topic:</b> {item.homeworkTopic}</p>
      <p><b>Notes:</b> {item.notes || '-'}</p>
      <p><b>Status:</b> {item.status}</p>
      <select value={item.status} onChange={(e) => updateStatus(e.target.value as SubmissionStatus)}>
        <option value="NEW">New</option>
        <option value="REVIEWED">Reviewed</option>
        <option value="ARCHIVED">Archived</option>
      </select>
      <h3>Files</h3>
      <ul>
        {item.files.map((f) => (
          <li key={f.id}><a href={`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/admin/files/${f.id}/download`} target="_blank">{f.originalFileName}</a></li>
        ))}
      </ul>
    </section>
  )
}
