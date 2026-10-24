import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'

export function BookDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('bookDetail.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('bookDetail.subtitle')}</p>
      </header>

      <section className="rounded-xl border bg-card p-6 text-card-foreground">
        <p className="text-sm text-muted-foreground">{t('bookDetail.bookId')}</p>
        <p className="font-mono text-sm">{id}</p>
        <p className="mt-4 text-xs text-muted-foreground">{t('common.comingSoon')} · M3</p>
      </section>
    </div>
  )
}
