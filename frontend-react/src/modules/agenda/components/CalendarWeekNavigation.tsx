import dayjs from 'dayjs'
import { Button } from '../../../components/ui/Button'

type CalendarWeekNavigationProps = {
  currentDate: string
  today: string
  itemsCount: number
  onDateChange: (date: string) => void
}

export function CalendarWeekNavigation({
  currentDate,
  today,
  itemsCount,
  onDateChange,
}: CalendarWeekNavigationProps) {
  const weekStart = dayjs(currentDate).startOf('day')
  const daysFromMonday = (weekStart.day() + 6) % 7
  const monday = weekStart.subtract(daysFromMonday, 'day')
  const sunday = monday.add(6, 'day')
  const isCurrentWeek = monday.isSame(dayjs(today).startOf('day').subtract((dayjs(today).day() + 6) % 7, 'day'), 'day')

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-1 pb-3">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-1">
          <Button
            onClick={() => onDateChange(dayjs(currentDate).subtract(7, 'day').format('YYYY-MM-DD'))}
            variant="secondary"
            className="!px-2 !py-1.5 !text-xs"
            aria-label="Semana anterior"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </Button>
          <Button
            onClick={() => onDateChange(today)}
            variant={isCurrentWeek ? 'primary' : 'secondary'}
            className="!px-3 !py-1.5 !text-xs font-semibold"
          >
            Hoy
          </Button>
          <Button
            onClick={() => onDateChange(dayjs(currentDate).add(7, 'day').format('YYYY-MM-DD'))}
            variant="secondary"
            className="!px-2 !py-1.5 !text-xs"
            aria-label="Semana siguiente"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </Button>
        </div>
        <div>
          <h2 className="text-sm font-bold text-slate-900">
            {monday.format('D MMM')} - {sunday.format('D MMM, YYYY')}
          </h2>
          <p className="text-xs text-slate-500">
            {itemsCount} {itemsCount === 1 ? 'reserva' : 'reservas'} en esta semana
          </p>
        </div>
      </div>
    </div>
  )
}
