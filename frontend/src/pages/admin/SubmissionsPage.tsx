import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { Submission, SubmissionStatus } from '../../types'
import { useTranslation } from 'react-i18next'

export function SubmissionsPage() {
  const { t } = useTranslation()
  const [items, setItems] = useState<Submission[]>([])
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<SubmissionStatus | ''>('')
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/api/admin/auth/me').catch(() => navigate('/admin/login'))
  }, [navigate])

  useEffect(() => {
    const params = new URLSearchParams()
    if (search) params.append('search', search)
    if (status) params.append('status', status)
    api.get(`/api/admin/submissions?${params.toString()}`).then((r) => setItems(r.data))
  }, [search, status])

  const groupedClasses = useMemo(() => [...new Set(items.map((i) => i.classGroup))], [items])

  return (
    <section className="card">
      <div className="row-between">
        <h2>{t('adminSubmissions')}</h2>
        <button onClick={() => { localStorage.removeItem('adminToken'); navigate('/admin/login')}}>{t('logout')}</button>
      </div>
      <div className="filters">
        <input placeholder={t('search')} value={search} onChange={(e) => setSearch(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value as SubmissionStatus | '')}>
          <option value="">{t('status')}</option>
          <option value="NEW">New</option>
          <option value="REVIEWED">Reviewed</option>
          <option value="ARCHIVED">Archived</option>
        </select>
        <select>
          <option>{t('filters')} - Class</option>
          {groupedClasses.map((g) => <option key={g}>{g}</option>)}
        </select>
      </div>
      <table>
        <thead><tr><th>ID</th><th>{t('studentName')}</th><th>{t('classGroup')}</th><th>{t('homeworkTopic')}</th><th>{t('status')}</th></tr></thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td><Link to={`/admin/submissions/${item.id}`}>#{item.id}</Link></td>
              <td>{item.studentName}</td>
              <td>{item.classGroup}</td>
              <td>{item.homeworkTopic}</td>
              <td>{item.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
