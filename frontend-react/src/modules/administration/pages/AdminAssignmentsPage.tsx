import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { StatusBadge as Badge } from '../../../components/ui/StatusBadge'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import {
  assignProfessionalToServiceRequest,
  assignRoomToServiceRequest,
  listAssignmentsRequest,
  removeAssignmentRequest,
} from '../../../services/api/assignmentsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { listAestheticServices } from '../../../services/api/aestheticApi'
import { listProfessionalsRequest } from '../../../services/api/professionalsApi'
import { listRoomsRequest } from '../../../services/api/roomsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { AssignmentResponse } from '../../../services/api/types'

type Tab = 'all' | 'professional-service' | 'room-service'

export function AdminAssignmentsPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [activeTab, setActiveTab] = useState<Tab>('all')
  const [filterServiceId, setFilterServiceId] = useState('')

  const assignmentsQuery = useQuery({
    queryKey: ['administration', 'assignments', filterServiceId],
    queryFn: () => listAssignmentsRequest({ serviceId: filterServiceId || undefined }),
  })

  const servicesQuery = useQuery({
    queryKey: ['administration', 'services', 'all'],
    queryFn: () => listAestheticServices({ size: 200 }),
  })

  const removeMutation = useMutation({
    mutationFn: (assignmentId: string) => removeAssignmentRequest(assignmentId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'assignments'] })
      showToast({
        title: 'Asignacion eliminada',
        description: 'La asignacion se elimino correctamente.',
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Error al eliminar',
        description: error instanceof ApiClientError ? error.message : 'No se pudo eliminar la asignacion.',
        tone: 'error',
      })
    },
  })

  const assignments = assignmentsQuery.data ?? []
  const serviceOptions = [
    { label: 'Todos los servicios', value: '' },
    ...(servicesQuery.data?.items ?? []).map((s) => ({ label: s.name, value: s.id })),
  ]

  const filteredByTab = assignments.filter((a) => {
    if (activeTab === 'professional-service') return a.assignmentType === 'PROFESSIONAL_SERVICE'
    if (activeTab === 'room-service') return a.assignmentType === 'ROOM_SERVICE'
    return true
  })

  const tabs = [
    { label: 'Todas', value: 'all' as Tab },
    { label: 'Profesional-Servicio', value: 'professional-service' as Tab },
    { label: 'Cabina-Servicio', value: 'room-service' as Tab },
  ]

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link to="/admin">
            <Button variant="secondary">Volver</Button>
          </Link>
        }
        description="Asigna profesionales y cabinas a servicios del catalogo."
        eyebrow="Administración"
        title="Asignaciones"
      />

      <Card className="overflow-hidden p-0">
        <div className="border-b border-[var(--color-border)]">
          <div className="flex flex-wrap">
            {tabs.map((tab) => (
              <button
                className={[
                  'px-4 py-3 text-sm font-medium transition',
                  activeTab === tab.value
                    ? 'border-b-2 border-blue-500 text-blue-700'
                    : 'text-slate-500 hover:text-slate-700',
                ].join(' ')}
                key={tab.value}
                onClick={() => setActiveTab(tab.value)}
                type="button"
              >
                {tab.label}
                <Badge
                  className="ml-2"
                  label={String(
                    tab.value === 'all'
                      ? assignments.length
                      : assignments.filter((a) =>
                          tab.value === 'professional-service'
                            ? a.assignmentType === 'PROFESSIONAL_SERVICE'
                            : a.assignmentType === 'ROOM_SERVICE',
                        ).length,
                  )}
                  tone="neutral"
                />
              </button>
            ))}
          </div>
        </div>

        <div className="p-4">
          <Select
            label="Filtrar por servicio"
            onChange={(event) => setFilterServiceId(event.target.value)}
            options={serviceOptions}
            value={filterServiceId}
          />
        </div>
      </Card>

      {assignmentsQuery.isPending ? (
        <LoadingState message="Cargando asignaciones." variant="page" />
      ) : null}

      {assignmentsQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar las asignaciones."
          onRetry={() => void assignmentsQuery.refetch()}
          title="No fue posible cargar asignaciones"
        />
      ) : null}

      {assignmentsQuery.data && filteredByTab.length > 0 ? (
        <div className="space-y-3">
          {filteredByTab.map((assignment) => (
            <AssignmentCard
              key={assignment.id}
              assignment={assignment}
              onDelete={() => removeMutation.mutate(assignment.id)}
              isDeleting={removeMutation.isPending}
            />
          ))}
        </div>
      ) : null}

      {assignmentsQuery.data && filteredByTab.length === 0 ? (
        <div className="rounded-[20px] border border-dashed border-slate-300 p-8 text-center">
          <p className="text-sm font-medium text-slate-600">
            No hay asignaciones {activeTab !== 'all' ? 'de este tipo' : ''}.
          </p>
          <p className="mt-1 text-sm text-slate-400">
            Usa los formularios de la seccion "Crear asignacion" para asociar profesionales y cabinas
            a servicios.
          </p>
        </div>
      ) : null}

      <div className="grid gap-6 md:grid-cols-2">
        <CreateProfessionalAssignmentCard />
        <CreateRoomAssignmentCard />
      </div>
    </section>
  )
}

function AssignmentCard({
  assignment,
  onDelete,
  isDeleting,
}: {
  assignment: AssignmentResponse
  onDelete: () => void
  isDeleting: boolean
}) {
  const isProfessional = assignment.assignmentType === 'PROFESSIONAL_SERVICE'
  return (
    <Card className="flex items-start justify-between gap-4 p-4">
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <Badge
            label={isProfessional ? 'Profesional' : 'Cabina'}
            tone={isProfessional ? 'info' : 'warning'}
          />
          <p className="truncate text-sm font-semibold text-slate-950">
            {assignment.serviceName}
          </p>
        </div>
        <p className="mt-2 text-sm text-slate-600">
          {isProfessional ? (
            <>
              Profesional: <span className="font-medium">{assignment.professionalName}</span>
            </>
          ) : (
            <>
              Cabina: <span className="font-medium">{assignment.roomName}</span>
              <span className="ml-2 text-slate-400">({assignment.roomCode})</span>
            </>
          )}
        </p>
      </div>
      <Button
        loading={isDeleting}
        onClick={onDelete}
        variant="secondary"
      >
        Eliminar
      </Button>
    </Card>
  )
}

function CreateProfessionalAssignmentCard() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [serviceId, setServiceId] = useState('')
  const [professionalId, setProfessionalId] = useState('')

  const servicesQuery = useQuery({
    queryKey: ['administration', 'services', 'all'],
    queryFn: () => listAestheticServices({ size: 200 }),
  })
  const professionalsQuery = useQuery({
    queryKey: ['administration', 'professionals', 'all'],
    queryFn: () => listProfessionalsRequest({ size: 200, active: true }),
  })

  const mutation = useMutation({
    mutationFn: () =>
      assignProfessionalToServiceRequest({
        serviceId,
        professionalId,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'assignments'] })
      showToast({
        title: 'Asignacion creada',
        description: 'Profesional asignado al servicio.',
        tone: 'success',
      })
      setServiceId('')
      setProfessionalId('')
    },
    onError: (error) => {
      showToast({
        title: 'Error',
        description: error instanceof ApiClientError ? error.message : 'No se pudo crear la asignacion.',
        tone: 'error',
      })
    },
  })

  const serviceOptions = [
    { label: 'Selecciona servicio', value: '' },
    ...(servicesQuery.data?.items ?? []).map((s) => ({ label: s.name, value: s.id })),
  ]
  const professionalOptions = [
    { label: 'Selecciona profesional', value: '' },
    ...(professionalsQuery.data?.items ?? []).map((p) => ({
      label: p.displayName ?? p.fullName,
      value: p.id,
    })),
  ]

  return (
    <Card className="p-4">
      <p className="mb-4 text-sm font-semibold text-slate-950">
        Asignar profesional a servicio
      </p>
      <div className="space-y-3">
        <Select
          label="Servicio"
          onChange={(event) => setServiceId(event.target.value)}
          options={serviceOptions}
          value={serviceId}
        />
        <Select
          label="Profesional"
          onChange={(event) => setProfessionalId(event.target.value)}
          options={professionalOptions}
          value={professionalId}
        />
        <Button
          disabled={!serviceId || !professionalId}
          loading={mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          Asignar
        </Button>
      </div>
    </Card>
  )
}

function CreateRoomAssignmentCard() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [serviceId, setServiceId] = useState('')
  const [roomId, setRoomId] = useState('')

  const servicesQuery = useQuery({
    queryKey: ['administration', 'services', 'all'],
    queryFn: () => listAestheticServices({ size: 200 }),
  })
  const roomsQuery = useQuery({
    queryKey: ['administration', 'rooms', 'all'],
    queryFn: () => listRoomsRequest({ size: 200, active: true }),
  })

  const mutation = useMutation({
    mutationFn: () =>
      assignRoomToServiceRequest({
        serviceId,
        roomId,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'assignments'] })
      showToast({
        title: 'Asignacion creada',
        description: 'Cabina asignada al servicio.',
        tone: 'success',
      })
      setServiceId('')
      setRoomId('')
    },
    onError: (error) => {
      showToast({
        title: 'Error',
        description: error instanceof ApiClientError ? error.message : 'No se pudo crear la asignacion.',
        tone: 'error',
      })
    },
  })

  const serviceOptions = [
    { label: 'Selecciona servicio', value: '' },
    ...(servicesQuery.data?.items ?? []).map((s) => ({ label: s.name, value: s.id })),
  ]
  const roomOptions = [
    { label: 'Selecciona cabina', value: '' },
    ...(roomsQuery.data?.items ?? []).map((r) => ({
      label: `${r.name} (${r.code})`,
      value: r.id,
    })),
  ]

  return (
    <Card className="p-4">
      <p className="mb-4 text-sm font-semibold text-slate-950">
        Asignar cabina a servicio
      </p>
      <div className="space-y-3">
        <Select
          label="Servicio"
          onChange={(event) => setServiceId(event.target.value)}
          options={serviceOptions}
          value={serviceId}
        />
        <Select
          label="Cabina"
          onChange={(event) => setRoomId(event.target.value)}
          options={roomOptions}
          value={roomId}
        />
        <Button
          disabled={!serviceId || !roomId}
          loading={mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          Asignar
        </Button>
      </div>
    </Card>
  )
}
