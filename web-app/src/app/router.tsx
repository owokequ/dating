import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router'
import { AppLayout } from './ui/AppLayout'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { RegisterPage } from '../features/auth/pages/RegisterPage'
import { VerifyEmailPage } from '../features/auth/pages/VerifyEmailPage'
import { ResetPasswordPage } from '../features/auth/pages/ResetPasswordPage'
import { TelegramCallbackPage } from '../features/auth/pages/TelegramCallbackPage'
import { DashboardPage } from '../features/dashboard/pages/DashboardPage'
import { CouplePage } from '../features/couple/pages/CouplePage'
import { InvitationPage } from '../features/invitation/pages/InvitationPage'
import { NewDatePage } from '../features/dates/pages/NewDatePage'
import { PrivatePlaceDatePage } from '../features/dates/pages/PrivatePlaceDatePage'
import { DateDetailsPage } from '../features/dates/pages/DateDetailsPage'
import { PlacesPage } from '../features/places/pages/PlacesPage'
import { PlaceDetailsPage } from '../features/places/pages/PlaceDetailsPage'
import { SettingsPage } from '../features/settings/pages/SettingsPage'
import { NotificationsPage } from '../features/notifications/pages/NotificationsPage'
import { AdminPlacesPage } from '../features/admin/places/pages/AdminPlacesPage'
import { EventsPage } from '../features/events/pages/EventsPage'
import { EventDetailsPage } from '../features/events/pages/EventDetailsPage'
import { AdminEventsPage } from '../features/admin/events/pages/AdminEventsPage'

const rootRoute = createRootRoute({
  component: AppLayout,
  notFoundComponent: () => (
    <section className="panel empty-state">
      <span className="eyebrow">404</span>
      <h1>Такой страницы нет</h1>
      <a className="button" href="/dashboard">Вернуться на главную</a>
    </section>
  ),
})

const route = (path: string, component: () => React.JSX.Element) =>
  createRoute({ getParentRoute: () => rootRoute, path, component })

const routeTree = rootRoute.addChildren([
  route('/', DashboardPage),
  route('/dashboard', DashboardPage),
  route('/register', RegisterPage),
  route('/login', LoginPage),
  route('/verify-email', VerifyEmailPage),
  route('/reset-password', ResetPasswordPage),
  route('/auth/telegram/callback', TelegramCallbackPage),
  route('/invite/$token', InvitationPage),
  route('/couple', CouplePage),
  route('/dates/new', NewDatePage),
  route('/dates/new/private-place', PrivatePlaceDatePage),
  route('/dates/$dateId', DateDetailsPage),
  route('/places', PlacesPage),
  route('/places/$placeId', PlaceDetailsPage),
  route('/events', EventsPage),
  route('/events/$eventId', EventDetailsPage),
  route('/settings', SettingsPage),
  route('/notifications', NotificationsPage),
  route('/admin/places', AdminPlacesPage),
  route('/admin/events', AdminEventsPage),
])

export const router = createRouter({ routeTree, defaultPreload: 'intent' })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
