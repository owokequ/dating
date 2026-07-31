export const formatDateTime = (value: string) => new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'long',
  timeStyle: 'short',
  timeZone: 'Europe/Moscow',
}).format(new Date(value))

export const toMoscowIso = (localValue: string) => new Date(`${localValue}:00+03:00`).toISOString()
