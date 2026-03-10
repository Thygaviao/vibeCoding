import { PropsWithChildren } from 'react'
import { LanguageSwitcher } from '../components/LanguageSwitcher'

export function MainLayout({ children }: PropsWithChildren) {
  return (
    <div className="app-shell">
      <header className="header card">
        <div className="logo-placeholder">[Logo]</div>
        <h1>Ms Ly English</h1>
        <LanguageSwitcher />
      </header>
      <main>{children}</main>
    </div>
  )
}
