import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { ApiError } from '../../../shared/api/http'
import { formatDateTime } from '../../../shared/lib/date'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { acceptInvitation, previewInvitation } from '../api/invitationApi'

export function InvitationPage() {
  const { token } = useParams({ strict: false }) as { token: string }
  const queryClient = useQueryClient()
  const preview = useQuery({ queryKey: ['invitation', token], queryFn: () => previewInvitation(token), retry: false })
  const accept = useMutation({
    mutationFn: () => acceptInvitation(token),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['couple'] })
      window.location.assign('/dashboard')
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        window.location.assign(`/login?continue=${encodeURIComponent(`/invite/${token}`)}`)
      }
    },
  })
  if (preview.isLoading) return <Loading>Проверяем приглашение…</Loading>

  return (
    <section className="auth-card panel">
      <PageTitle eyebrow="Приглашение" title="Стать парой в Owoke">После принятия вы сможете предлагать и подтверждать свидания.</PageTitle>
      {preview.data ? (
        <>
          <p>Ссылка действительна до <strong>{formatDateTime(preview.data.expiresAt)}</strong>.</p>
          <button onClick={() => accept.mutate()} disabled={accept.isPending}>Принять приглашение</button>
          <p className="muted">Если вы ещё не вошли, мы сохраним эту страницу и вернём вас после входа.</p>
        </>
      ) : <ErrorMessage error={preview.error} />}
      <ErrorMessage error={accept.error} />
    </section>
  )
}
