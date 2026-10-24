import { MessageSquare } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export function ChatPage() {
  const { t } = useTranslation()

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t('chat.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('chat.subtitle')}</p>
      </header>

      <section className="flex min-h-80 flex-col items-center justify-center gap-3 rounded-xl border border-dashed bg-muted/30 p-10 text-center">
        <div className="rounded-full bg-background p-3 shadow-sm">
          <MessageSquare className="size-8 text-muted-foreground" />
        </div>
        <h2 className="text-lg font-medium">{t('chat.emptyTitle')}</h2>
        <p className="max-w-md text-sm text-muted-foreground">{t('chat.emptyDescription')}</p>
        <span className="text-xs text-muted-foreground">{t('chat.comingSoon')}</span>
      </section>
    </div>
  )
}
