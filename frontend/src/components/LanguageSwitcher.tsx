import { useTranslation } from 'react-i18next'

export function LanguageSwitcher() {
  const { i18n } = useTranslation()
  return (
    <div className="lang-switch">
      <button onClick={() => i18n.changeLanguage('en')} className={i18n.language === 'en' ? 'active' : ''}>EN</button>
      <button onClick={() => i18n.changeLanguage('vi')} className={i18n.language === 'vi' ? 'active' : ''}>VI</button>
    </div>
  )
}
