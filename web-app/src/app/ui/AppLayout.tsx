import { Outlet } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { logout, useSession } from '../../features/auth/api/authApi'

export function AppLayout() {
  const session = useSession()
  const queryClient = useQueryClient()
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      queryClient.clear()
      window.location.assign('/login')
    },
  })

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/dashboard" aria-label="Owoke — на главную">
          <span className="brand-mark">O</span>
          <span>owoke</span>
        </a>
        <nav aria-label="Основная навигация">
          {session.data ? (
            <>
              <a href="/places">Места</a>
              <a href="/events">Афиша</a>
              <a href="/couple">Пара</a>
              <a href="/notifications">Уведомления</a>
              <a href="/settings">Настройки</a>
              {session.data.role === 'ADMIN' && <>
                <a className="admin-link" href="/admin/places">Admin · места</a>
                <a className="admin-link" href="/admin/events">Admin · афиша</a>
              </>}
              <button className="link-button" onClick={() => logoutMutation.mutate()}>Выйти</button>
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
      <footer>Казань · Europe/Moscow · обычный web-сайт</footer>
    </div>
  )
}
