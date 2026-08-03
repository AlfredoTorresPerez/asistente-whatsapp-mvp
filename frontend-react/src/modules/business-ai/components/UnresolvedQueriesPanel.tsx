import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import type { AestheticIntentLogResponse } from '../../../services/api/types'

type AuditEntry = {
  id: string
  title: string
  category: string
  status: string
  updatedAt: string
  description: string
  type: string
  log?: AestheticIntentLogResponse
}

type Props = {
  entries: AuditEntry[]
  page: number
  totalPages: number
  totalLogs: number
  onPageChange: (page: number) => void
  onSelectEntry: (entry: AuditEntry) => void
  isLoading: boolean
}

function formatReviewStatus(status: string) {
  switch (status) {
    case 'pending':
    case 'requires handoff':
      return 'Pendiente'
    case 'resolved':
    case 'synced':
    case 'active':
      return 'Resuelta'
    default:
      return 'Por revisar'
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function UnresolvedQueriesPanel({
  entries,
  page,
  totalPages,
  totalLogs,
  onPageChange,
  onSelectEntry,
  isLoading,
}: Props) {
  return (
    <Card className="p-4">
      <h2 className="text-lg font-semibold">Historial de respuestas</h2>
      <p className="mt-1 text-xs text-gray-500">
        Últimas respuestas generadas por el asistente. Haz clic en una para revisarla en el panel de prueba.
      </p>

      {isLoading ? (
        <div className="mt-4 flex justify-center py-8 text-gray-400">Cargando...</div>
      ) : entries.length === 0 ? (
        <div className="mt-4 flex justify-center py-8 text-gray-400">
          No hay respuestas registradas aún.
        </div>
      ) : (
        <div className="mt-3 space-y-2">
          {entries.map((entry) => (
            <button
              key={entry.id}
              onClick={() => onSelectEntry(entry)}
              className="w-full rounded-lg border border-gray-100 p-3 text-left transition-colors hover:bg-gray-50"
            >
              <div className="grid gap-2 text-xs text-gray-600 md:grid-cols-4">
                <ReviewField label="Fecha" value={formatDate(entry.updatedAt)} />
                <ReviewField label="Cliente" value="Cliente por identificar" />
                <ReviewField label="Confianza" value={`${Math.round((entry.log?.confidence ?? 0) * 100)}%`} />
                <ReviewField label="Estado" value={formatReviewStatus(entry.status)} />
              </div>
              <div className="mt-3 grid gap-2 text-xs text-gray-600 md:grid-cols-2">
                <ReviewField label="Pregunta" value={entry.log?.sourceMessage ?? entry.description} />
                <ReviewField label="Respuesta" value={entry.log?.suggestedResponse ?? entry.description} />
                <ReviewField label="Regla aplicada" value={entry.category} />
                <ReviewField
                  label="Motivo de revisión"
                  value={entry.log?.handoffReason ?? 'Control de calidad del asistente'}
                />
                <ReviewField label="Corrección propuesta" value="Pendiente de revisión" />
                <ReviewField label="Responsable" value="Sin responsable asignado" />
              </div>
            </button>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-3 flex items-center justify-between text-sm text-gray-500">
          <span>
            Página {page + 1} de {totalPages} ({totalLogs} registros)
          </span>
          <div className="flex gap-1">
            <Button
              variant="secondary"
              size="sm"
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}
            >
              Anterior
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
            >
              Siguiente
            </Button>
          </div>
        </div>
      )}
    </Card>
  )
}

function ReviewField({ label, value }: { label: string; value: string }) {
  return (
    <span className="block">
      <span className="block font-semibold text-gray-500">{label}</span>
      <span className="line-clamp-2 text-gray-800">{value}</span>
    </span>
  )
}
