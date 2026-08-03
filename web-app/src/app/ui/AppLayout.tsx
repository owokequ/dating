import { Outlet, useRouterState } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Bell, CalendarHeart, Compass, Heart, LogOut, MapPin, Settings, Sparkles } from 'lucide-react'
import { logout, useSession } from '../../features/auth/api/authApi'
import { OnboardingOverlay } from '../../features/onboarding/ui/OnboardingOverlay'

const primaryNavigation = [
  { href: '/dashboard', label: 'Сегодня', icon: CalendarHeart },
  { href: '/places', label: 'Места', icon: MapPin },
  { href: '/events', label: 'Афиша', icon: Sparkles },
  { href: '/couple', label: 'Мы', icon: Heart },
] as const

export function AppLayout() {
  const session = useSession()
  const queryClient = useQueryClient()
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      queryClient.clear()
      window.location.assign('/login')
    },
  })

  const active = (href: string) => pathname === href || (href !== '/dashboard' && pathname.startsWith(`${href}/`))

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/dashboard" aria-label="For my L — на главную">
          <span className="brand-mark" aria-hidden="true">L</span>
          <span className="brand-copy"><strong>For my L</strong><small>наше место</small></span>
        </a>
        <nav className="desktop-nav" aria-label="Основная навигация">
          {session.data ? (
            <>
              <div className="nav-primary">
                {primaryNavigation.slice(1).map(({ href, label }) => (
                  <a className={active(href) ? 'active' : undefined} href={href} key={href}>{label}</a>
                ))}
              </div>
              {session.data.role === 'ADMIN' && <>
                <a className="admin-link" href="/admin/places">Admin</a>
              </>}
              <div className="nav-tools">
                <a className="icon-link" href="/notifications" aria-label="Уведомления"><Bell size={18} /></a>
                <a className="icon-link" href="/settings" aria-label="Настройки"><Settings size={18} /></a>
                <button className="icon-link link-button" aria-label="Выйти" onClick={() => logoutMutation.mutate()}><LogOut size={18} /></button>
              </div>
            </>
          ) : (
            <>
              <a href="/login">Войти</a>
              <a className="nav-cta" href="/register">Создать аккаунт</a>
            </>
          )}
        </nav>
      </header>
      <main><Outlet /></main>
      <footer><span>For my L</span><span className="footer-heart">♡</span><span>Казань</span></footer>
      <OnboardingOverlay />
      {session.data && (
        <nav className="mobile-nav" aria-label="Мобильная навигация">
          {primaryNavigation.map(({ href, label, icon: Icon }) => (
            <a className={active(href) ? 'active' : undefined} href={href} key={href} aria-current={active(href) ? 'page' : undefined}>
              <Icon size={21} strokeWidth={active(href) ? 2.4 : 1.8} />
              <span>{label}</span>
            </a>
          ))}
          <a className="mobile-nav-create" href="/dates/new" aria-label="Предложить свидание"><Compass size={22} /></a>
        </nav>
      )}
    </div>
  )
}
