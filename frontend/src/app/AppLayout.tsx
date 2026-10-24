import { BookOpen, Library, MessageSquare, Search, Settings } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { NavLink, Outlet } from 'react-router-dom'

import { cn } from '@/shared/lib/utils'

const navItems = [
  { to: '/', labelKey: 'nav.library', icon: Library, end: true },
  { to: '/search', labelKey: 'nav.search', icon: Search, end: false },
  { to: '/chat', labelKey: 'nav.chat', icon: MessageSquare, end: false },
  { to: '/settings', labelKey: 'nav.settings', icon: Settings, end: false },
]

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors',
    isActive
      ? 'bg-sidebar-accent text-sidebar-accent-foreground'
      : 'text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
  )

export function AppLayout() {
  const { t, i18n } = useTranslation()

  return (
    <div className="flex min-h-screen flex-col bg-background md:flex-row">
      <aside className="flex shrink-0 flex-col border-b bg-sidebar md:w-64 md:border-r md:border-b-0">
        <div className="flex items-center gap-3 px-4 py-4">
          <div className="rounded-lg bg-primary p-2 text-primary-foreground">
            <BookOpen className="size-5" />
          </div>
          <div>
            <p className="font-semibold leading-tight">{t('app.name')}</p>
            <p className="text-xs text-muted-foreground">{t('app.tagline')}</p>
          </div>
        </div>

        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col md:overflow-visible">
          {navItems.map(({ to, labelKey, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={navLinkClassName}>
              <Icon className="size-4" />
              {t(labelKey)}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto space-y-1 border-t px-4 py-4">
          <label htmlFor="language" className="text-xs font-medium text-muted-foreground">
            {t('common.language')}
          </label>
          <select
            id="language"
            value={i18n.resolvedLanguage ?? 'vi'}
            onChange={(event) => void i18n.changeLanguage(event.target.value)}
            className="w-full rounded-md border bg-background px-2 py-1.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
          >
            <option value="vi">Tiếng Việt</option>
            <option value="en">English</option>
          </select>
        </div>
      </aside>

      <main className="flex-1 p-6 md:p-10">
        <div className="mx-auto w-full max-w-5xl">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
