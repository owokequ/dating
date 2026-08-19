import { useQuery } from '@tanstack/react-query'
import { CalendarPlus, Check, HeartHandshake, MessageCircleHeart, Sparkles, X } from 'lucide-react'
import { useState } from 'react'
import { useRouterState } from '@tanstack/react-router'
import { useSession } from '../../auth/api/authApi'
import { getCurrentCouple } from '../../couple/api/coupleApi'

type GuideStep = {
  action: string
  description: string
  href: string
  icon: typeof HeartHandshake
  progress: number
  title: string
}

const dismissedKey = (userId: string) => `owoke-onboarding-dismissed:${userId}`

export function OnboardingOverlay() {
  const session = useSession()
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const [isDismissed, setIsDismissed] = useState(false)
  const canGuide = Boolean(session.data && !session.data.onboardingCompleted && pathname === '/dashboard')
  const couple = useQuery({
    queryKey: ['couple'],
    queryFn: getCurrentCouple,
    enabled: canGuide,
  })

  const hasBeenDismissed = session.data
    ? sessionStorage.getItem(dismissedKey(session.data.userId)) === 'true'
    : false
  if (!session.data || !canGuide || couple.isLoading || isDismissed || hasBeenDismissed) return null

  const userId = session.data.userId
  const step: GuideStep = !couple.data
    ? {
        title: 'Создайте ваше пространство',
        description: 'Отправьте личную ссылку партнёру — после этого сможете планировать свидания вместе.',
        action: 'Пригласить партнёра',
        href: '/couple',
        icon: HeartHandshake,
        progress: 0,
      }
    : couple.data.status !== 'ACTIVE'
      ? {
          title: 'Приглашение уже ждёт ответа',
          description: 'Откройте пару, чтобы скопировать ссылку или проверить, когда ваш человек присоединится.',
          action: 'Открыть пару',
          href: '/couple',
          icon: Sparkles,
          progress: 1,
        }
      : {
          title: 'Создайте первое свидание',
          description: 'Выберите время, добавьте личную фотографию и отправьте красивое приглашение.',
          action: 'Создать приглашение',
          href: '/dates/new',
          icon: CalendarPlus,
          progress: 2,
        }
  const StepIcon = step.icon
  const dismiss = () => {
    sessionStorage.setItem(dismissedKey(userId), 'true')
    setIsDismissed(true)
  }

  return (
    <aside className="onboarding-overlay" role="dialog" aria-modal="true" aria-labelledby="onboarding-title">
      <section className="onboarding-card">
        <button className="onboarding-close" type="button" aria-label="Закрыть обучение" onClick={dismiss}><X size={18} /></button>
        <div className="onboarding-orbit" aria-hidden="true"><Sparkles size={17} /></div>
        <span className="onboarding-kicker">Ваш первый маршрут</span>
        <div className="onboarding-progress" aria-label={`Шаг ${step.progress + 1} из 3`}>
          {[0, 1, 2].map((index) => <span className={index <= step.progress ? 'complete' : undefined} key={index}>{index < step.progress && <Check size={12} />}</span>)}
        </div>
        <div className="onboarding-step-icon"><StepIcon size={25} /></div>
        <h2 id="onboarding-title">{step.title}</h2>
        <p>{step.description}</p>
        {!session.data.telegramLinked && <p className="onboarding-note"><MessageCircleHeart size={16} />Telegram можно подключить позже — он нужен только для напоминаний.</p>}
        <a className="button" href={step.href} onClick={dismiss}>{step.action}</a>
        <button className="onboarding-dismiss" type="button" onClick={dismiss}>Продолжу позже</button>
      </section>
    </aside>
  )
}
