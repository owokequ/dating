import { useQuery } from '@tanstack/react-query'
import { useSession } from '../../auth/api/authApi'
import { getCurrentCouple } from '../../couple/api/coupleApi'

export function OnboardingOverlay() {
  const session = useSession()
  const couple = useQuery({ queryKey: ['couple'], queryFn: getCurrentCouple, enabled: Boolean(session.data) })
  if (!session.data || session.data.onboardingCompleted) return null
  const step = !session.data.telegramLinked
    ? { title: 'Сначала свяжем вас с ботом', text: 'Подключите Telegram — туда придут красивые приглашения и личные напоминания.', href: '/settings', action: 'Открыть настройки' }
    : !couple.data
      ? { title: 'Пригласите своего человека', text: 'Создайте личную ссылку и отправьте её партнёру.', href: '/couple', action: 'Создать приглашение' }
      : couple.data.status !== 'ACTIVE'
        ? { title: 'Ждём вашего человека', text: 'Как только партнёр примет ссылку, продолжим с первым свиданием.', href: '/couple', action: 'Открыть пару' }
        : { title: 'Создайте первое свидание', text: 'Выберите место, время и отправьте приглашение партнёру.', href: '/dates/new', action: 'Предложить свидание' }
  return <aside className="onboarding-overlay" role="dialog" aria-modal="true">
    <div className="onboarding-card"><span className="eyebrow">Первый шаг</span><h2>{step.title}</h2><p>{step.text}</p><a className="button" href={step.href}>{step.action}</a></div>
  </aside>
}
