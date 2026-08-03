import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { FilterBar } from '../../../components/ui/FilterBar'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { usePermissions } from '../../../hooks/usePermissions'
import { listAestheticServiceCategories, listAestheticServices } from '../../../services/api/aestheticApi'
import {
  getAssignmentsSummaryRequest,
  listAssignmentGroupsRequest,
  type AssignmentGroupsParams,
} from '../../../services/api/assignmentsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { listProfessionalsRequest } from '../../../services/api/professionalsApi'
import { listRoomsRequest } from '../../../services/api/roomsApi'
import { AssignmentsCoveragePanel } from './assignments/AssignmentsCoveragePanel'
import { AssignmentGroupsList } from './assignments/AssignmentGroupsList'
import { AssignmentsSummaryCards } from './assignments/AssignmentsSummaryCards'
import { CreateAssignmentDialog } from './assignments/CreateAssignmentDialog'

const PAGE_SIZE = 10

const coverageOptions = [
  { label: 'Todas las coberturas', value: '' },
  { label: 'Con cobertura', value: 'covered' },
  { label: 'Cobertura parcial', value: 'partial' },
  { label: 'Sin asignar', value: 'none' },
]

export function AdminAssignmentsPage() {
  const { hasPermission } = usePermissions()
  const canManage = hasPermission('ASSIGNMENT_MANAGE')

  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [locationId, setLocationId] = useState('')
  const [categoryCode, setCategoryCode] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [professionalId, setProfessionalId] = useState('')
  const [roomId, setRoomId] = useState('')
  const [coverage, setCoverage] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [createServiceId, setCreateServiceId] = useState<string | undefined>()
  const [createSession, setCreateSession] = useState(0)

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput)
      setPage(0)
    }, 400)
    return () => clearTimeout(timer)
  }, [searchInput])

  const groupsQuery = useQuery({
    queryKey: [
      'administration',
      'assignments',
      'groups',
      page,
      search,
      locationId,
      categoryCode,
      serviceId,
      professionalId,
      roomId,
      coverage,
    ],
    queryFn: () =>
      listAssignmentGroupsRequest({
        page,
        search: search || undefined,
        locationId: locationId || undefined,
        categoryCode: categoryCode || undefined,
        serviceId: serviceId || undefined,
        professionalId: professionalId || undefined,
        roomId: roomId || undefined,
        size: PAGE_SIZE,
        coverage: coverage as AssignmentGroupsParams['coverage'],
      }),
    placeholderData: keepPreviousData,
  })

  const summaryQuery = useQuery({
    queryKey: ['administration', 'assignments', 'summary'],
    queryFn: getAssignmentsSummaryRequest,
  })

  const servicesQuery = useQuery({
    queryKey: ['administration', 'services', 'all'],
    queryFn: () => listAestheticServices({ size: 200 }),
  })

  const categoriesQuery = useQuery({
    queryKey: ['administration', 'service-categories', 'active'],
    queryFn: () => listAestheticServiceCategories({ size: 200, active: true }),
  })

  const locationsQuery = useQuery({
    queryKey: ['administration', 'locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const professionalsQuery = useQuery({
    queryKey: ['administration', 'professionals', 'active'],
    queryFn: () => listProfessionalsRequest({ size: 200, active: true }),
  })

  const roomsQuery = useQuery({
    queryKey: ['administration', 'rooms', 'active'],
    queryFn: () => listRoomsRequest({ size: 200, active: true }),
  })

  const serviceOptions = [
    { label: 'Todos los servicios', value: '' },
    ...(servicesQuery.data?.items ?? []).map((s) => ({ label: s.name, value: s.id })),
  ]
  const locationOptions = [
    { label: 'Todas las sedes', value: '' },
    ...(locationsQuery.data ?? []).map((l) => ({ label: l.name, value: l.id })),
  ]
  const categoryOptions = [
    { label: 'Todas las categorias', value: '' },
    ...(categoriesQuery.data?.items ?? []).map((c) => ({ label: c.name, value: c.code })),
  ]
  const professionalOptions = [
    { label: 'Todos los profesionales', value: '' },
    ...(professionalsQuery.data?.items ?? []).map((p) => ({
      label: p.displayName ?? p.fullName,
      value: p.id,
    })),
  ]
  const roomOptions = [
    { label: 'Todas las cabinas', value: '' },
    ...(roomsQuery.data?.items ?? []).map((r) => ({ label: r.name, value: r.id })),
  ]

  const groups = groupsQuery.data?.items ?? []
  const totalPages = Math.max(groupsQuery.data?.totalPages ?? 1, 1)

  const openCreate = (prefillServiceId?: string) => {
    setCreateServiceId(prefillServiceId)
    setCreateSession((current) => current + 1)
    setCreateOpen(true)
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            {canManage ? (
              <Button onClick={() => openCreate()}>Asignar profesional o cabina</Button>
            ) : null}
            <Link to="/admin">
              <Button variant="secondary">Volver</Button>
            </Link>
          </>
        }
        description="Asigna profesionales y cabinas a servicios del catalogo y revisa la cobertura de agenda."
        eyebrow="Administración"
        title="Asignaciones"
      />

      <AssignmentsSummaryCards summary={summaryQuery.data} />

      <FilterBar>
        <Input
          label="Buscar"
          onChange={(event) => setSearchInput(event.target.value)}
          placeholder="Servicio, profesional o cabina"
          value={searchInput}
        />
        <Select
          label="Sede"
          onChange={(event) => {
            setLocationId(event.target.value)
            setPage(0)
          }}
          options={locationOptions}
          value={locationId}
        />
        <Select
          label="Categoria"
          onChange={(event) => {
            setCategoryCode(event.target.value)
            setPage(0)
          }}
          options={categoryOptions}
          value={categoryCode}
        />
        <Select
          label="Servicio"
          onChange={(event) => {
            setServiceId(event.target.value)
            setPage(0)
          }}
          options={serviceOptions}
          value={serviceId}
        />
        <Select
          label="Profesional"
          onChange={(event) => {
            setProfessionalId(event.target.value)
            setPage(0)
          }}
          options={professionalOptions}
          value={professionalId}
        />
        <Select
          label="Cabina"
          onChange={(event) => {
            setRoomId(event.target.value)
            setPage(0)
          }}
          options={roomOptions}
          value={roomId}
        />
        <Select
          label="Cobertura"
          onChange={(event) => {
            setCoverage(event.target.value)
            setPage(0)
          }}
          options={coverageOptions}
          value={coverage}
        />
      </FilterBar>

      {groupsQuery.isPending ? (
        <LoadingState message="Cargando asignaciones." variant="page" />
      ) : null}

      {groupsQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar las asignaciones."
          onRetry={() => void groupsQuery.refetch()}
          title="No fue posible cargar asignaciones"
        />
      ) : null}

      {groupsQuery.data && groups.length > 0 ? (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
          <div className="min-w-0 space-y-4">
            <AssignmentGroupsList
              canManage={canManage}
              groups={groups}
              onAddProfessional={(serviceId) => openCreate(serviceId)}
              onAddRoom={(serviceId) => openCreate(serviceId)}
            />

            {totalPages > 1 ? (
              <div className="flex flex-col items-center justify-between gap-3 rounded-[24px] border border-[var(--color-border)] bg-white px-5 py-4 shadow-[var(--shadow-card)] sm:flex-row">
                <p className="text-[13px] text-[var(--color-text-secondary)]">
                  Pagina {page + 1} de {totalPages} · {PAGE_SIZE} servicios por pagina
                </p>
                <div className="flex gap-3">
                  <Button
                    disabled={page === 0 || groupsQuery.isFetching}
                    onClick={() => setPage((current) => Math.max(current - 1, 0))}
                    variant="secondary"
                  >
                    Anterior
                  </Button>
                  <Button
                    disabled={page >= totalPages - 1 || groupsQuery.isFetching}
                    onClick={() => setPage((current) => Math.min(current + 1, totalPages - 1))}
                    variant="secondary"
                  >
                    Siguiente
                  </Button>
                </div>
              </div>
            ) : null}
          </div>

          <div className="xl:sticky xl:top-6 xl:self-start">
            <AssignmentsCoveragePanel groups={groups} />
          </div>
        </div>
      ) : null}

      {groupsQuery.data && groups.length === 0 ? (
        <div className="space-y-4">
          <EmptyState
            description="Asigna profesionales y cabinas a los servicios del catalogo para habilitar su agenda."
            title="No hay asignaciones"
          />
          {canManage ? (
            <div className="flex justify-center">
              <Button onClick={() => openCreate()}>Asignar profesional o cabina</Button>
            </div>
          ) : null}
        </div>
      ) : null}

      <CreateAssignmentDialog
        defaultServiceId={createServiceId}
        key={`${createSession}:${createServiceId ?? ''}`}
        onClose={() => setCreateOpen(false)}
        open={createOpen}
      />
    </section>
  )
}
