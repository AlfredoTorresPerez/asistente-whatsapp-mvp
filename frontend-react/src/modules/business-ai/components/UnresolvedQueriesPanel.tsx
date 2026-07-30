import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'

type AuditEntry = {
  id: string
  title: string
  category: string
  status: string
  updatedAt: string
  description: string
  type: string
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
        Últimas respuestas generadas por el asistente. Haz clic en una para cargarla en el simulador.
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
              <div className="flex items-center gap-2">
                <span
                  className={`inline-block h-2 w-2 rounded-full ${
                    entry.status === 'requires handoff'
                      ? 'bg-amber-400'
                      : entry.status === 'active' || entry.status === 'synced'
                        ? 'bg-green-400'
                        : 'bg-blue-400'
                  }`}
                />
                <span className="text-sm font-medium">{entry.category}</span>
              </div>
              <p className="mt-1 line-clamp-2 text-xs text-gray-600">{entry.title}</p>
              <p className="mt-1 text-xs text-gray-400">{entry.updatedAt}</p>
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
