import dayjs from 'dayjs'

const currencyFormatter = new Intl.NumberFormat('es-CL', {
  currency: 'CLP',
  maximumFractionDigits: 0,
  style: 'currency',
})

const numberFormatter = new Intl.NumberFormat('es-CL')

export function formatChileanDate(value: string | Date | null | undefined) {
  if (!value) return '---'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('DD-MM-YYYY') : '---'
}

export function formatChileanShortDate(value: string | Date | null | undefined) {
  if (!value) return '---'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('DD-MM') : '---'
}

export function formatChileanTime(value: string | Date | null | undefined) {
  if (!value) return '---'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('HH:mm') : '---'
}

export function formatChileanCurrency(value: number | null | undefined) {
  return currencyFormatter.format(Math.round(value ?? 0)).replace(/\s/g, '')
}

export function formatChileanNumber(value: number | null | undefined) {
  return numberFormatter.format(value ?? 0)
}

export function formatChileanPercent(value: number | null | undefined) {
  if (value === null || value === undefined) return 'Sin periodo anterior'
  return `${numberFormatter.format(value)}%`
}

export function formatMaskedPhone(phone: string | null | undefined) {
  if (!phone) return '---'
  if (phone.length <= 4) return phone
  const visible = phone.slice(-4)
  return `${phone.slice(0, -4).replace(/\d/g, '*')}${visible}`
}

export function formatMinutesAsHours(minutes: number | null | undefined) {
  const hours = Math.round(((minutes ?? 0) / 60) * 10) / 10
  return `${numberFormatter.format(hours)} h`
}
