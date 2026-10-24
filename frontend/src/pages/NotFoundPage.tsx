import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import { Button } from '@/shared/ui/button'

export function NotFoundPage() {
  const { t } = useTranslation()

  return (
    <div className="flex min-h-80 flex-col items-center justify-center gap-4 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">{t('notFound.title')}</h1>
      <p className="text-sm text-muted-foreground">{t('notFound.description')}</p>
      <Button asChild>
        <Link to="/">{t('notFound.backHome')}</Link>
      </Button>
    </div>
  )
}
