import { Search } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export function SearchPage() {
  const { t } = useTranslation()

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('search.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('search.subtitle')}</p>
      </header>

      <div className="flex items-center gap-2 rounded-lg border bg-background px-3 py-2">
        <Search className="size-4 text-muted-foreground" />
        <input
          type="search"
          disabled
          placeholder={t('search.placeholder')}
          className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed"
        />
      </div>

      <section className="flex min-h-64 flex-col items-center justify-center gap-2 rounded-xl border border-dashed bg-muted/30 p-10 text-center">
        <h2 className="text-lg font-medium">{t('search.emptyTitle')}</h2>
        <p className="max-w-md text-sm text-muted-foreground">{t('search.emptyDescription')}</p>
        <span className="text-xs text-muted-foreground">{t('search.comingSoon')}</span>
      </section>
    </div>
  )
}
