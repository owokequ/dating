import type { PropsWithChildren } from 'react'

export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null
  return <p className="message error" role="alert">{error instanceof Error ? error.message : 'Что-то пошло не так'}</p>
}

export function Loading({ children = 'Загружаем…' }: PropsWithChildren) {
  return <div className="panel loading" aria-live="polite">{children}</div>
}

export function PageTitle({ eyebrow, title, children }: PropsWithChildren<{ eyebrow: string; title: string }>) {
  return (
    <header className="page-title">
      <span className="eyebrow">{eyebrow}</span>
      <h1>{title}</h1>
      {children && <p>{children}</p>}
    </header>
  )
}
