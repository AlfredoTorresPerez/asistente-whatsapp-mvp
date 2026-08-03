import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useState } from 'react'
import { ConfirmDialog } from '../../../../components/overlay/ConfirmDialog'
import { Button } from '../../../../components/ui/Button'
import { Card } from '../../../../components/ui/Card'
import { StatusBadge } from '../../../../components/ui/StatusBadge'
import { useToast } from '../../../../lib/toast'
import { removeAssignmentRequest, setAssignmentActiveRequest } from '../../../../services/api/assignmentsApi'
import { ApiClientError } from '../../../../services/api/httpClient'
import type { AssignmentGroupResponse, AssignmentResponse } from '../../../../services/api/types'

type AssignmentGroupsListProps = {
  canManage: boolean
  groups: AssignmentGroupResponse[]
  onAddProfessional: (serviceId: string) => void
  onAddRoom: (serviceId: string) => void
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

export function AssignmentGroupsList({
  canManage,
  groups,
  onAddProfessional,
  onAddRoom,
}: AssignmentGroupsListProps) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [deleteTarget, setDeleteTarget] = useState<AssignmentResponse | null>(null)

  const toggleExpanded = (serviceId: string) => {
    setExpandedIds((current) => {
      const next = new Set(current)
      if (next.has(serviceId)) {
        next.delete(serviceId)
      } else {
        next.add(serviceId)
      }
      return next
    })
  }

  const toggleActiveMutation = useMutation({
    mutationFn: ({ assignmentId, active }: { assignmentId: string; active: boolean }) =>
      setAssignmentActiveRequest(assignmentId, { active }),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'assignments'] })
      showToast({
        title: 'Asignacion actualizada',
        description: updated.active ? 'La asignacion esta activa.' : 'La asignacion esta inactiva.',
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Error al actualizar',
        description: errorMessage(error, 'No se pudo actualizar la asignacion.'),
        tone: 'error',
      })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (assignmentId: string) => removeAssignmentRequest(assignmentId),
    onSuccess: async () => {
      setDeleteTarget(null)
      await queryClient.invalidateQueries({ queryKey: ['administration', 'assignments'] })
      showToast({
        title: 'Asignacion eliminada',
        description: 'La asignacion se elimino correctamente.',
        tone: 'success',
      })
    },
    onError: (error) => {
      setDeleteTarget(null)
      showToast({
        title: 'Error al eliminar',
        description: errorMessage(error, 'No se pudo eliminar la asignacion.'),
        tone: 'error',
      })
    },
  })

  return (
    <>
      <div className="space-y-4">
        {groups.map((group) => {
          const isExpanded = expandedIds.has(group.serviceId)
          return (
            <Card className="overflow-hidden p-0" key={group.serviceId}>
              <button
                aria-expanded={isExpanded}
                className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left transition hover:bg-[var(--color-muted-surface)]"
                onClick={() => toggleExpanded(group.serviceId)}
                type="button"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <span
                    className={[
                      'inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl transition-transform',
                      isExpanded ? 'rotate-90' : '',
                    ].join(' ')}
                  >
                    <svg
                      className="h-4 w-4 text-slate-400"
                      fill="none"
                      viewBox="0 0 24 24"
                      xmlns="http://www.w3.org/2000/svg"
                    >
                      <path
                        d="M9 6L15 12L9 18"
                        stroke="currentColor"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth="1.8"
                      />
                    </svg>
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-[var(--color-text)]">
                      {group.serviceName}
                    </p>
                    <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                      {group.categoryName} · {group.professionalsCount} profesional(es) · {group.roomsCount}{' '}
                      cabina(s)
                    </p>
                    {group.locationNames.length > 0 ? (
                      <p className="mt-0.5 truncate text-xs text-[var(--color-text-secondary)]">
                        {group.locationNames.join(', ')}
                      </p>
                    ) : null}
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <StatusBadge
                    label={group.covered ? 'Cubierto' : group.professionalsCount + group.roomsCount > 0 ? 'Parcial' : 'Sin asignar'}
                    tone={group.covered ? 'success' : group.professionalsCount + group.roomsCount > 0 ? 'warning' : 'neutral'}
                  />
                </div>
              </button>

              {isExpanded ? (
                <div className="grid gap-5 border-t border-[var(--color-divider)] px-5 py-5 md:grid-cols-2">
                  <AssignmentColumn
                    canManage={canManage}
                    emptyLabel="Sin profesionales asignados"
                    icon={
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M12 12C14.2 12 16 10.2 16 8C16 5.8 14.2 4 12 4C9.8 4 8 5.8 8 8C8 10.2 9.8 12 12 12Z"
                          stroke="currentColor"
                          strokeWidth="1.7"
                        />
                        <path
                          d="M4 20C4 16.8 7.6 14.5 12 14.5C16.4 14.5 20 16.8 20 20"
                          stroke="currentColor"
                          strokeLinecap="round"
                          strokeWidth="1.7"
                        />
                      </svg>
                    }
                    items={group.professionals}
                    onAdd={() => onAddProfessional(group.serviceId)}
                    onDelete={setDeleteTarget}
                    onToggle={(assignment) =>
                      toggleActiveMutation.mutate({ assignmentId: assignment.id, active: !assignment.active })
                    }
                    pending={toggleActiveMutation.isPending || removeMutation.isPending}
                    title="Profesionales"
                  />
                  <AssignmentColumn
                    canManage={canManage}
                    emptyLabel="Sin cabinas asignadas"
                    icon={
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M3 20V9L12 4L21 9V20"
                          stroke="currentColor"
                          strokeLinejoin="round"
                          strokeWidth="1.7"
                        />
                        <path
                          d="M3 20H21M7 20V14M17 20V14M7 14H17"
                          stroke="currentColor"
                          strokeLinejoin="round"
                          strokeWidth="1.7"
                        />
                        <path d="M10 9H14" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" />
                      </svg>
                    }
                    items={group.rooms}
                    onAdd={() => onAddRoom(group.serviceId)}
                    onDelete={setDeleteTarget}
                    onToggle={(assignment) =>
                      toggleActiveMutation.mutate({ assignmentId: assignment.id, active: !assignment.active })
                    }
                    pending={toggleActiveMutation.isPending || removeMutation.isPending}
                    title="Cabinas"
                  />
                </div>
              ) : null}
            </Card>
          )
        })}
      </div>

      <ConfirmDialog
        confirmLabel="Eliminar"
        confirmLoading={removeMutation.isPending}
        description={
          deleteTarget
            ? `Se eliminara la asignacion de ${deleteTarget.professionalName ?? deleteTarget.roomName ?? ''} al servicio ${deleteTarget.serviceName}. Esta accion no se puede deshacer.`
            : ''
        }
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            removeMutation.mutate(deleteTarget.id)
          }
        }}
        open={deleteTarget !== null}
        title="Eliminar asignacion"
        tone="danger"
      />
    </>
  )
}

type AssignmentColumnProps = {
  canManage: boolean
  emptyLabel: string
  icon: ReactNode
  items: AssignmentResponse[]
  onAdd: () => void
  onDelete: (assignment: AssignmentResponse) => void
  onToggle: (assignment: AssignmentResponse) => void
  pending: boolean
  title: string
}

function AssignmentColumn({
  canManage,
  emptyLabel,
  icon,
  items,
  onAdd,
  onDelete,
  onToggle,
  pending,
  title,
}: AssignmentColumnProps) {
  return (
    <div>
      <div className="flex items-center justify-between gap-2">
        <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.16em] text-[var(--color-text-secondary)]">
          <span className="text-[var(--color-primary)]">{icon}</span>
          {title}
        </p>
        {canManage ? (
          <Button onClick={onAdd} size="sm" variant="secondary">
            Asignar
          </Button>
        ) : null}
      </div>

      {items.length > 0 ? (
        <ul className="mt-3 space-y-2">
          {items.map((item) => (
            <li
              className="flex items-center justify-between gap-3 rounded-2xl border border-[var(--color-border)] bg-[var(--color-muted-surface)] px-3 py-2.5"
              key={item.id}
            >
              <div className="min-w-0">
                <p className="truncate text-[13px] font-medium text-[var(--color-text)]">
                  {item.professionalName ?? item.roomName}
                </p>
                <p className="mt-0.5 truncate text-xs text-[var(--color-text-secondary)]">
                  {item.roomCode ?? ''}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <StatusBadge
                  label={item.active ? 'Activo' : 'Inactivo'}
                  tone={item.active ? 'success' : 'neutral'}
                />
                {canManage ? (
                  <>
                    <button
                      aria-label={item.active ? 'Desactivar asignacion' : 'Activar asignacion'}
                      className="inline-flex h-8 w-8 items-center justify-center rounded-xl text-slate-400 transition hover:bg-white hover:text-[var(--color-primary)] disabled:opacity-50"
                      disabled={pending}
                      onClick={() => onToggle(item)}
                      type="button"
                    >
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M13 2L4.5 13.5H11L9.5 22L19 9.5H12.5L13 2Z"
                          stroke="currentColor"
                          strokeLinejoin="round"
                          strokeWidth="1.8"
                        />
                      </svg>
                    </button>
                    <button
                      aria-label="Eliminar asignacion"
                      className="inline-flex h-8 w-8 items-center justify-center rounded-xl text-slate-400 transition hover:bg-white hover:text-red-500 disabled:opacity-50"
                      disabled={pending}
                      onClick={() => onDelete(item)}
                      type="button"
                    >
                      <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M4 7H20M10 11V17M14 11V17M6 7L7 20H17L18 7M9 7V4H15V7"
                          stroke="currentColor"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth="1.8"
                        />
                      </svg>
                    </button>
                  </>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <div className="mt-3 flex items-center justify-between gap-3 rounded-2xl border border-dashed border-[var(--color-border)] px-3 py-3">
          <p className="text-xs text-[var(--color-text-secondary)]">{emptyLabel}</p>
          {canManage ? (
            <Button onClick={onAdd} size="sm" variant="secondary">
              Asignar
            </Button>
          ) : null}
        </div>
      )}
    </div>
  )
}
