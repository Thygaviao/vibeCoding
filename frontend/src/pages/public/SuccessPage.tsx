import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'

export function SuccessPage() {
  const { id } = useParams()
  const { t } = useTranslation()
  return (
    <section className="card">
      <h2>{t('success')}</h2>
      <p>Submission ID: #{id}</p>
      <Link className="primary inline" to="/">{t('back')}</Link>
    </section>
  )
}
