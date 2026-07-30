import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { StatusBadge } from '../../../components/ui/StatusBadge'

type KnowledgeRow = {
  id: string
  title: string
  category: string
  status: string
  updatedAt: string
  description: string
  type: string
}

type Tab = { label: string; value: string }

type Props = {
  activeTab: string
  onTabChange: (tab: string) => void
  tabs: readonly Tab[]
  rows: KnowledgeRow[]
  paginatedRows: KnowledgeRow[]
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  search: string
  onSearchChange: (v: string) => void
  statusFilter: string
  onStatusFilterChange: (v: string) => void
  onAdd: () => void
  onEdit: (row: KnowledgeRow) => void
  onToggleStatus: (row: KnowledgeRow) => void
  isLoading: boolean
  onOpenFullBase: () => void
}

export function BusinessInformationSummary({
  activeTab,
  onTabChange,
  tabs,
  rows,
  paginatedRows,
  page,
  totalPages,
  onPageChange,
  search,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  onAdd,
  onEdit,
  onToggleStatus,
  isLoading,
  onOpenFullBase,
}: Props) {
  return (
    <Card className="p-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Información del negocio</h2>
          <p className="mt-1 text-xs text-gray-500">
            Catálogo de servicios, productos, reglas y políticas que el asistente conoce sobre tu negocio.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={onOpenFullBase}>
            Ver todo
          </Button>
          <Button size="sm" onClick={onAdd}>
            Agregar
          </Button>
        </div>
      </div>

      <div className="mt-3 flex gap-1 border-b border-gray-200">
        {tabs.map((tab) => (
          <button
            key={tab.value}
            onClick={() => onTabChange(tab.value)}
            className={`px-3 py-2 text-sm font-medium transition-colors ${
              activeTab === tab.value
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-3 flex gap-2">
        <input
          type="text"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Buscar..."
          className="flex-1 rounded border border-gray-300 px-3 py-2 text-sm"
        />
        <select
          value={statusFilter}
          onChange={(e) => onStatusFilterChange(e.target.value)}
          className="rounded border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="all">Todos</option>
          <option value="active">Activos</option>
          <option value="inactive">Inactivos</option>
        </select>
      </div>

      {isLoading ? (
        <div className="mt-4 flex justify-center py-8 text-gray-400">Cargando...</div>
      ) : rows.length === 0 ? (
        <div className="mt-4 flex justify-center py-8 text-gray-400">
          No hay información disponible en esta sección.
        </div>
      ) : (
        <div className="mt-3 space-y-2">
          {paginatedRows.map((row) => (
            <div
              key={row.id}
              className="flex items-start justify-between rounded-lg border border-gray-100 p-3 transition-colors hover:bg-gray-50"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{row.title}</p>
                {row.description && (
                  <p className="mt-0.5 line-clamp-2 text-xs text-gray-500">{row.description}</p>
                )}
                <div className="mt-1 flex items-center gap-2">
                  <StatusBadge status={row.status} />
                  <span className="text-xs text-gray-400">{row.category}</span>
                  <span className="text-xs text-gray-400">{row.updatedAt}</span>
                </div>
              </div>
              <div className="ml-3 flex gap-1">
                <Button variant="secondary" size="sm" onClick={() => onEdit(row)}>
                  Editar
                </Button>
                {row.type !== 'audit' && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => onToggleStatus(row)}
                  >
                    {row.status === 'active' ? 'Desactivar' : 'Activar'}
                  </Button>
                )}
              </div>
            </div>
          ))}

          <div className="flex items-center justify-between pt-2 text-sm text-gray-500">
            <span>
              {rows.length > 0
                ? `Mostrando ${page * 10 + 1}-${Math.min((page + 1) * 10, rows.length)} de ${rows.length}`
                : 'Sin resultados'}
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
        </div>
      )}
    </Card>
  )
}
