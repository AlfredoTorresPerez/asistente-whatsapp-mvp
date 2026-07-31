import { Card } from '../../../../components/ui/Card'
import { StatusBadge } from '../../../../components/ui/StatusBadge'
import type { AssignmentGroupResponse } from '../../../../services/api/types'

type AssignmentsCoveragePanelProps = {
  groups: AssignmentGroupResponse[]
}

export function AssignmentsCoveragePanel({ groups }: AssignmentsCoveragePanelProps) {
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-semibold text-[var(--color-text)]">Cobertura por servicio</p>
        <StatusBadge label="Pagina actual" tone="neutral" />
      </div>
      <p className="mt-1 text-xs leading-5 text-[var(--color-text-secondary)]">
        Un servicio tiene cobertura cuando cuenta con al menos un profesional y una cabina activos.
      </p>

      {groups.length > 0 ? (
        <ul className="mt-4 divide-y divide-[var(--color-divider)]">
          {groups.map((group) => (
            <li className="flex items-center justify-between gap-3 py-3" key={group.serviceId}>
              <div className="min-w-0">
                <p className="truncate text-[13px] font-medium text-[var(--color-text)]">
                  {group.serviceName}
                </p>
                <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                  {group.professionalsCount} profesional(es) · {group.roomsCount} cabina(s)
                </p>
              </div>
              <StatusBadge
                label={group.covered ? 'Cubierto' : group.professionalsCount + group.roomsCount > 0 ? 'Parcial' : 'Sin asignar'}
                tone={group.covered ? 'success' : group.professionalsCount + group.roomsCount > 0 ? 'warning' : 'neutral'}
              />
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-4 text-sm text-[var(--color-text-secondary)]">
          No hay servicios en esta pagina para mostrar cobertura.
        </p>
      )}
    </Card>
  )
}
