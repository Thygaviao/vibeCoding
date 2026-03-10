import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'

const allowed = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf']

export function SubmissionPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [studentName, setStudentName] = useState('')
  const [classGroup, setClassGroup] = useState('')
  const [homeworkTopic, setHomeworkTopic] = useState('')
  const [notes, setNotes] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!studentName || !classGroup || !homeworkTopic) return setError(t('required'))
    if (!files.length) return setError(t('fileRequired'))
    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('studentName', studentName)
      formData.append('classGroup', classGroup)
      formData.append('homeworkTopic', homeworkTopic)
      formData.append('notes', notes)
      files.forEach((file) => formData.append('files', file))
      const { data } = await api.post('/api/submissions', formData)
      navigate(`/success/${data.id}`)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Submit failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="card">
      <h2>{t('submitHomework')}</h2>
      <form onSubmit={onSubmit} className="form-grid">
        <input placeholder={t('studentName')} value={studentName} onChange={(e) => setStudentName(e.target.value)} required />
        <input placeholder={t('classGroup')} value={classGroup} onChange={(e) => setClassGroup(e.target.value)} required />
        <input placeholder={t('homeworkTopic')} value={homeworkTopic} onChange={(e) => setHomeworkTopic(e.target.value)} required />
        <textarea placeholder={t('notes')} value={notes} onChange={(e) => setNotes(e.target.value)} />
        <input
          type="file"
          multiple
          onChange={(e) => {
            const selected = Array.from(e.target.files ?? [])
              .filter((f) => allowed.includes(f.type))
              .slice(0, 10)
            setFiles(selected)
          }}
        />
        <ul>
          {files.map((f, idx) => (
            <li key={`${f.name}-${idx}`}>
              {f.name}
              <button type="button" onClick={() => setFiles(files.filter((_, i) => i !== idx))}>x</button>
            </li>
          ))}
        </ul>
        {error && <p className="error">{error}</p>}
        <button className="primary" type="submit" disabled={loading}>{loading ? '...' : t('submit')}</button>
      </form>
    </section>
  )
}
