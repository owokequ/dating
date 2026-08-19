import { Outlet, useRouterState } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Bell, CalendarHeart, Compass, Heart, LogOut, MapPin, Moon, MoreHorizontal, Settings, Sparkles, Sun } from 'lucide-react'
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
  const [theme, setTheme] = useState<'light' | 'dark'>(() => document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light')
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      queryClient.clear()
      window.location.assign('/login')
    },
  })

  const active = (href: string) => pathname === href || (href !== '/dashboard' && pathname.startsWith(`${href}/`))
  const toggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'
    document.documentElement.dataset.theme = nextTheme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', nextTheme === 'dark' ? '#21171d' : '#fff4f7')
    localStorage.setItem('owoke-theme', nextTheme)
    setTheme(nextTheme)
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/dashboard" aria-label="For my L — на главную">
          <img className="brand-mark" src="/brand-mark.svg" alt="" />
          <span className="brand-copy"><strong>For my L</strong><small>наше место</small></span>
        </a>
        <nav className="desktop-nav" aria-label="Основная навигация">
          <button
            className="icon-link link-button theme-toggle"
            type="button"
            aria-label={theme === 'dark' ? 'Enable light mode' : 'Enable dark mode'}
            aria-pressed={theme === 'dark'}
            onClick={toggleTheme}
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>
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
          <details className="mobile-more">
            <summary aria-label="Ещё разделы">
              <MoreHorizontal size={21} />
              <span>Ещё</span>
            </summary>
            <div className="mobile-more-panel">
              <a href="/notifications"><Bell size={18} />Уведомления</a>
              <a href="/settings"><Settings size={18} />Настройки</a>
              {session.data.role === 'ADMIN' && <a href="/admin/places"><Compass size={18} />Управление</a>}
              <button type="button" onClick={() => logoutMutation.mutate()}><LogOut size={18} />Выйти</button>
            </div>
          </details>
          <a className="mobile-nav-create" href="/dates/new" aria-label="Предложить свидание"><Compass size={22} /></a>
        </nav>
      )}
    </div>
  )
}
