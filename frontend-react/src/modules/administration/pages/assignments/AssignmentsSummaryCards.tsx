import type { ReactNode } from 'react'
import { Card } from '../../../../components/ui/Card'
import type { AssignmentSummaryResponse } from '../../../../services/api/types'

type MetricCardProps = {
  icon: ReactNode
  label: string
  value: number
  iconClass: string
}

function MetricCard({ icon, iconClass, label, value }: MetricCardProps) {
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-3xl font-semibold text-[var(--color-text)]">{value}</p>
          <p className="mt-2 text-[13px] font-medium text-[var(--color-text-secondary)]">{label}</p>
        </div>
        <span
          className={[
            'inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl',
            iconClass,
          ]
            .join(' ')
            .trim()}
        >
          {icon}
        </span>
      </div>
    </Card>
  )
}

const iconClassNames = {
  info: 'bg-blue-50 text-blue-600',
  success: 'bg-emerald-50 text-emerald-600',
  warning: 'bg-amber-50 text-amber-600',
  neutral: 'bg-slate-100 text-slate-500',
}

export function AssignmentsSummaryCards({ summary }: { summary: AssignmentSummaryResponse | undefined }) {
  const metrics = [
    {
      icon: (
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M12 3L20 7V17L12 21L4 17V7L12 3Z"
            stroke="currentColor"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
          <path d="M12 12L20 8M12 12L4 8M12 12V21" stroke="currentColor" strokeLinejoin="round" strokeWidth="1.8" />
        </svg>
      ),
      iconClass: iconClassNames.info,
      label: 'Servicios en catalogo',
      value: summary?.totalServices ?? 0,
    },
    {
      icon: (
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M20 6L9 17L4 12"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
        </svg>
      ),
      iconClass: iconClassNames.success,
      label: 'Con cobertura',
      value: summary?.coveredServices ?? 0,
    },
    {
      icon: (
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M5 3V9M5 9L3.5 7.5M5 9L6.5 7.5M19 3V9M19 9L17.5 7.5M19 9L20.5 7.5"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
          <path d="M4 14H20V20H4V14Z" stroke="currentColor" strokeLinejoin="round" strokeWidth="1.8" />
          <path d="M8 17H16" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      ),
      iconClass: iconClassNames.warning,
      label: 'Cobertura parcial',
      value: summary?.partialServices ?? 0,
    },
    {
      icon: (
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M12 8V12M12 16H12.01"
            stroke="currentColor"
            strokeLinecap="round"
            strokeWidth="1.8"
          />
          <path
            d="M12 3L21 20H3L12 3Z"
            stroke="currentColor"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
        </svg>
      ),
      iconClass: iconClassNames.neutral,
      label: 'Sin asignar',
      value: summary?.uncoveredServices ?? 0,
    },
  ]

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric) => (
        <MetricCard
          icon={metric.icon}
          iconClass={metric.iconClass}
          key={metric.label}
          label={metric.label}
          value={metric.value}
        />
      ))}
    </div>
  )
}
