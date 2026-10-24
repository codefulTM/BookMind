import { Settings } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export function SettingsPage() {
  const { t } = useTranslation()

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('settings.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('settings.subtitle')}</p>
      </header>

      <section className="rounded-xl border bg-card p-6 text-card-foreground">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-muted p-2">
            <Settings className="size-5 text-muted-foreground" />
          </div>
          <div className="space-y-1">
            <h2 className="font-medium">{t('settings.aiTitle')}</h2>
            <p className="text-sm text-muted-foreground">{t('settings.aiDescription')}</p>
            <p className="text-xs text-muted-foreground">{t('settings.comingSoon')}</p>
          </div>
        </div>
      </section>
    </div>
  )
}
