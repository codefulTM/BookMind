import { BookOpen, Plus } from 'lucide-react'
import { useTranslation } from 'react-i18next'

import { Button } from '@/shared/ui/button'

export function LibraryPage() {
  const { t } = useTranslation()

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('library.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('library.subtitle')}</p>
      </header>

      <section className="flex min-h-80 flex-col items-center justify-center gap-4 rounded-xl border border-dashed bg-muted/30 p-10 text-center">
        <div className="rounded-full bg-background p-3 shadow-sm">
          <BookOpen className="size-8 text-muted-foreground" />
        </div>
        <div className="space-y-1">
          <h2 className="text-lg font-medium">{t('library.emptyTitle')}</h2>
          <p className="max-w-md text-sm text-muted-foreground">{t('library.emptyDescription')}</p>
        </div>
        <Button disabled>
          <Plus className="size-4" />
          {t('library.addBook')}
        </Button>
        <span className="text-xs text-muted-foreground">{t('common.comingSoon')} · M2</span>
      </section>
    </div>
  )
}
