import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import type { SummaryCard, ReadinessCheck } from '../hooks/useBusinessReadiness'

type Props = {
  summaryCards: SummaryCard[]
  readinessChecks: ReadinessCheck[]
  passedCount: number
  totalChecks: number
  isLoading: boolean
}

function SummaryCardView({ card }: { card: SummaryCard }) {
  const hasIssues = card.warnings.length > 0
  return (
    <div className="flex flex-col gap-1 rounded-lg border border-gray-200 p-4 transition-colors hover:border-gray-300">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">{card.label}</span>
        <span className={`inline-flex h-6 min-w-[24px] items-center justify-center rounded-full px-2 text-xs font-semibold ${
          card.activeCount > 0
            ? 'bg-green-50 text-green-700'
            : 'bg-gray-100 text-gray-500'
        }`}>
          {card.activeCount}/{card.count}
        </span>
      </div>
      {hasIssues && (
        <div className="flex flex-wrap gap-1">
          {card.warnings.map((w, i) => (
            <span key={i} className="inline-flex items-center rounded-full bg-amber-50 px-2 py-0.5 text-[11px] text-amber-700">
              {w}
            </span>
          ))}
        </div>
      )}
      <Link
        to={card.adminLink}
        className="mt-1 text-xs font-medium text-blue-600 hover:text-blue-800 hover:underline"
      >
        {card.adminLabel} →
      </Link>
    </div>
  )
}

function ReadinessCheckRow({ check }: { check: ReadinessCheck }) {
  return (
    <div className="flex items-start gap-3 rounded-lg border border-gray-100 p-3 transition-colors hover:bg-gray-50">
      <span className={`mt-0.5 text-sm ${check.passed ? 'text-green-500' : 'text-red-400'}`}>
        {check.passed ? '✓' : '✗'}
      </span>
      <div className="min-w-0 flex-1">
        <p className={`text-sm font-medium ${check.passed ? 'text-gray-700' : 'text-gray-900'}`}>
          {check.label}
        </p>
        {check.detail && (
          <p className="mt-0.5 text-xs text-gray-500">{check.detail}</p>
        )}
      </div>
    </div>
  )
}

const EMOJI_MAP: Record<string, string> = {
  services: '💇',
  products: '🧴',
  promotions: '🏷️',
  locations: '🏢',
  professionals: '👤',
  rooms: '🚪',
  schedules: '🕐',
  policies: '📋',
}

export function BusinessInformationSummary({
  summaryCards,
  readinessChecks,
  passedCount,
  totalChecks,
  isLoading,
}: Props) {
  if (isLoading) {
    return (
      <Card className="p-4">
        <h2 className="text-lg font-semibold">Información del negocio</h2>
        <div className="mt-4 flex justify-center py-8 text-gray-400">Cargando...</div>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <Card className="p-4">
        <div>
          <h2 className="text-lg font-semibold">Información del negocio</h2>
          <p className="mt-1 text-xs text-gray-500">
            Resumen de la información que el asistente conoce sobre tu negocio. Para editar, usa los mantenedores oficiales.
          </p>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {summaryCards.map((card) => (
            <SummaryCardView key={card.key} card={card} />
          ))}
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="text-lg font-semibold">Preparación del asistente</h2>
        <p className="mt-1 text-sm text-gray-600">
          {passedCount} de {totalChecks} verificaciones correctas.
        </p>

        <div className="mt-1">
          <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
            <div
              className={`h-full rounded-full transition-all ${
                passedCount === totalChecks
                  ? 'bg-green-500'
                  : passedCount >= totalChecks / 2
                    ? 'bg-amber-400'
                    : 'bg-red-400'
              }`}
              style={{ width: `${totalChecks > 0 ? (passedCount / totalChecks) * 100 : 0}%` }}
            />
          </div>
        </div>

        <div className="mt-4 space-y-2">
          {readinessChecks.map((check) => (
            <ReadinessCheckRow key={check.key} check={check} />
          ))}
        </div>
      </Card>
    </div>
  )
}
