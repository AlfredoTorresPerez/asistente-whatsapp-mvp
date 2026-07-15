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

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 p-4">
      <div>
        <h2 className="text-sm font-semibold text-slate-900">Calendario semanal</h2>
        <p className="mt-0.5 text-xs text-slate-500">
          {monday.format('DD/MM/YYYY')} - {sunday.format('DD/MM/YYYY')} &middot; {itemsCount}{' '}
          reservas
        </p>
      </div>
      <div className="flex flex-wrap gap-1.5">
        <Button
          onClick={() => onDateChange(dayjs(currentDate).subtract(7, 'day').format('YYYY-MM-DD'))}
          variant="secondary"
          className="!px-2.5 !py-1 !text-xs"
        >
          &larr; Ant.
        </Button>
        <Button
          onClick={() => onDateChange(today)}
          variant="secondary"
          className="!px-2.5 !py-1 !text-xs"
        >
          Hoy
        </Button>
        <Button
          onClick={() => onDateChange(dayjs(currentDate).add(7, 'day').format('YYYY-MM-DD'))}
          variant="secondary"
          className="!px-2.5 !py-1 !text-xs"
        >
          Sig. &rarr;
        </Button>
      </div>
    </div>
  )
}
