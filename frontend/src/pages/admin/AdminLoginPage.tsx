import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'

export function AdminLoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const { data } = await api.post('/api/admin/auth/login', { username, password })
      localStorage.setItem('adminToken', data.token)
      navigate('/admin/submissions')
    } catch {
      setError('Invalid credentials')
    }
  }

  return (
    <section className="card">
      <h2>{t('login')}</h2>
      <form className="form-grid" onSubmit={submit}>
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder={t('username')} />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder={t('password')} />
        {error && <p className="error">{error}</p>}
        <button className="primary" type="submit">{t('login')}</button>
      </form>
    </section>
  )
}
