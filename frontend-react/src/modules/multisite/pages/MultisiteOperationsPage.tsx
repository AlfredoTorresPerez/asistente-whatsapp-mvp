import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import {
  createMultisiteScheduleRequest,
  getMultisiteCatalogAvailabilityRequest,
  getMultisiteChannelsRequest,
  getMultisiteProfessionalsRequest,
  getMultisiteSchedulesRequest,
  getMultisiteSummaryRequest,
  getMultisiteUserAccessRequest,
  updateMultisiteCatalogAvailabilityRequest,
  updateMultisiteChannelLocationRequest,
  updateMultisiteUserAccessRequest,
} from '../../../services/api/multisiteApi'
import type {
  MultisiteCatalogAvailabilityResponse,
  MultisiteChannelResponse,
  MultisiteLocationSummaryResponse,
  MultisiteProfessionalResponse,
  ProfessionalScheduleResponse,
  UpsertCatalogAvailabilityRequest,
  UpsertProfessionalScheduleRequest,
  UserLocationAccessResponse,
} from '../../../services/api/types'

type ActiveTab = 'summary' | 'catalog' | 'professionals' | 'permissions' | 'channels' | 'validation'

type ScheduleForm = {
  professionalId: string
  locationId: string
  dayOfWeek: string
  startTime: string
  endTime: string
}

type CatalogForm = {
  productServiceId: string
  locationId: string
  stockQuantity: string
  stockMinimum: string
  priceOverride: string
}

const emptyScheduleForm: ScheduleForm = {
  professionalId: '',
  locationId: '',
  dayOfWeek: '1',
  startTime: '09:00',
  endTime: '18:00',
}

const emptyCatalogForm: CatalogForm = {
  productServiceId: '',
  locationId: '',
  stockQuantity: '0',
  stockMinimum: '0',
  priceOverride: '',
}

export function MultisiteOperationsPage() {
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState<ActiveTab>('summary')
  const [selectedLocationId, setSelectedLocationId] = useState<string>('')
  const [scheduleForm, setScheduleForm] = useState<ScheduleForm>(emptyScheduleForm)
  const [catalogForm, setCatalogForm] = useState<CatalogForm>(emptyCatalogForm)

  const summaryQuery = useQuery({ queryKey: ['multisite', 'summary'], queryFn: getMultisiteSummaryRequest })
  const catalogQuery = useQuery({
    queryKey: ['multisite', 'catalog-availability', selectedLocationId],
    queryFn: () => getMultisiteCatalogAvailabilityRequest({ locationId: selectedLocationId || null }),
  })
  const professionalsQuery = useQuery({ queryKey: ['multisite', 'professionals'], queryFn: getMultisiteProfessionalsRequest })
  const schedulesQuery = useQuery({
    queryKey: ['multisite', 'professional-schedules', selectedLocationId],
    queryFn: () => getMultisiteSchedulesRequest({ locationId: selectedLocationId || null }),
  })
  const userAccessQuery = useQuery({ queryKey: ['multisite', 'user-access'], queryFn: getMultisiteUserAccessRequest })
  const channelsQuery = useQuery({ queryKey: ['multisite', 'channels'], queryFn: getMultisiteChannelsRequest })

  const locations = summaryQuery.data ?? []
  const catalogItems = useMemo(() => catalogQuery.data ?? [], [catalogQuery.data])
  const professionals = professionalsQuery.data ?? []
  const schedules = schedulesQuery.data ?? []
  const userAccess = userAccessQuery.data ?? []
  const channels = channelsQuery.data ?? []

  const catalogOptions = useMemo(() => {
    const seen = new Map<string, MultisiteCatalogAvailabilityResponse>()
    for (const item of catalogItems) {
      if (!seen.has(item.itemId)) {
        seen.set(item.itemId, item)
      }
    }
    return Array.from(seen.values())
  }, [catalogItems])

  const saveScheduleMutation = useMutation({
    mutationFn: (payload: UpsertProfessionalScheduleRequest) => createMultisiteScheduleRequest(payload),
    onSuccess: () => {
      showToast({ title: 'Horario guardado', description: 'El horario por sede quedo disponible para validar agenda.', tone: 'success' })
      setScheduleForm(emptyScheduleForm)
      void queryClient.invalidateQueries({ queryKey: ['multisite', 'professional-schedules'] })
      void queryClient.invalidateQueries({ queryKey: ['multisite', 'professionals'] })
    },
    onError: () => showToast({ title: 'No se pudo guardar horario', description: 'Verifica sede, profesional y rango horario.', tone: 'error' }),
  })

  const saveCatalogMutation = useMutation({
    mutationFn: (payload: UpsertCatalogAvailabilityRequest) => updateMultisiteCatalogAvailabilityRequest(payload),
    onSuccess: () => {
      showToast({ title: 'Disponibilidad actualizada', description: 'El catalogo por sede fue actualizado.', tone: 'success' })
      setCatalogForm(emptyCatalogForm)
      void queryClient.invalidateQueries({ queryKey: ['multisite', 'catalog-availability'] })
      void queryClient.invalidateQueries({ queryKey: ['multisite', 'summary'] })
    },
    onError: () => showToast({ title: 'No se pudo actualizar catalogo', description: 'Revisa producto, sede y stock.', tone: 'error' }),
  })

  const updateAccessMutation = useMutation({
    mutationFn: (access: UserLocationAccessResponse) => updateMultisiteUserAccessRequest({
      userId: access.userId,
      locationId: access.locationId,
      roleScope: access.roleScope,
      canViewConversations: !access.canViewConversations,
      canManageBookings: access.canManageBookings,
      canManageOrders: access.canManageOrders,
      canManageCatalog: access.canManageCatalog,
      canViewReports: access.canViewReports,
      active: true,
    }),
    onSuccess: () => {
      showToast({ title: 'Permiso actualizado', description: 'El acceso por sede fue actualizado.', tone: 'success' })
      void queryClient.invalidateQueries({ queryKey: ['multisite', 'user-access'] })
    },
    onError: () => showToast({ title: 'No se pudo actualizar permiso', description: 'Revisa el usuario y la sede.', tone: 'error' }),
  })

  const updateChannelMutation = useMutation({
    mutationFn: (channel: MultisiteChannelResponse) => updateMultisiteChannelLocationRequest(channel.channelId, {
      locationId: channel.locationId,
      routingMode: channel.locationId ? 'LOCATION_SPECIFIC' : 'CENTRALIZED',
    }),
    onSuccess: () => {
      showToast({ title: 'Canal actualizado', description: 'La configuracion de canal por sede fue persistida.', tone: 'success' })
      void queryClient.invalidateQueries({ queryKey: ['multisite', 'channels'] })
    },
    onError: () => showToast({ title: 'No se pudo actualizar canal', description: 'El canal puede tener una restriccion de unicidad por sede.', tone: 'error' }),
  })

  const isLoading = summaryQuery.isPending || catalogQuery.isPending || professionalsQuery.isPending || schedulesQuery.isPending || userAccessQuery.isPending || channelsQuery.isPending
  const hasError = summaryQuery.isError || catalogQuery.isError || professionalsQuery.isError || schedulesQuery.isError || userAccessQuery.isError || channelsQuery.isError

  const submitSchedule = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!scheduleForm.professionalId || !scheduleForm.locationId) {
      showToast({ title: 'Faltan datos', description: 'Selecciona profesional y sede.', tone: 'error' })
      return
    }
    saveScheduleMutation.mutate({
      professionalId: scheduleForm.professionalId,
      locationId: scheduleForm.locationId,
      dayOfWeek: Number(scheduleForm.dayOfWeek),
      startTime: scheduleForm.startTime,
      endTime: scheduleForm.endTime,
      active: true,
    })
  }

  const submitCatalog = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!catalogForm.productServiceId || !catalogForm.locationId) {
      showToast({ title: 'Faltan datos', description: 'Selecciona producto/servicio y sede.', tone: 'error' })
      return
    }
    saveCatalogMutation.mutate({
      productServiceId: catalogForm.productServiceId,
      locationId: catalogForm.locationId,
      active: true,
      stockEnabled: true,
      stockQuantity: Number(catalogForm.stockQuantity || 0),
      stockMinimum: Number(catalogForm.stockMinimum || 0),
      priceOverride: catalogForm.priceOverride ? Number(catalogForm.priceOverride) : null,
    })
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/admin/locations"><Button variant="secondary">Sedes del negocio</Button></Link>
            <Link to="/reports"><Button variant="secondary">Reportes</Button></Link>
            <Link to="/appointments"><Button>Ver agenda</Button></Link>
          </div>
        }
        description="Centro operacional para sedes, disponibilidad, stock, profesionales, permisos y canales WhatsApp por sucursal."
        eyebrow="Multisede"
        title="Operacion multisede completa"
      />

      {isLoading ? <LoadingState message="Cargando configuracion multisede." variant="page" /> : null}
      {hasError ? <ErrorState title="No fue posible cargar operacion multisede" description="Revisa que el backend tenga aplicada la migracion V18 y que la sesion este vigente." /> : null}

      {!hasError ? (
        <>
          <LocationSelector locations={locations} value={selectedLocationId} onChange={setSelectedLocationId} />
          <TabBar activeTab={activeTab} onChange={setActiveTab} />

          {activeTab === 'summary' ? <SummaryPanel locations={locations} /> : null}
          {activeTab === 'catalog' ? (
            <CatalogPanel
              catalogForm={catalogForm}
              catalogItems={catalogItems}
              catalogOptions={catalogOptions}
              locations={locations}
              onChange={setCatalogForm}
              onSubmit={submitCatalog}
              saving={saveCatalogMutation.isPending}
            />
          ) : null}
          {activeTab === 'professionals' ? (
            <ProfessionalsPanel
              locations={locations}
              professionals={professionals}
              scheduleForm={scheduleForm}
              schedules={schedules}
              saving={saveScheduleMutation.isPending}
              onChange={setScheduleForm}
              onSubmit={submitSchedule}
            />
          ) : null}
          {activeTab === 'permissions' ? (
            <PermissionsPanel access={userAccess} onToggle={(access) => updateAccessMutation.mutate(access)} updating={updateAccessMutation.isPending} />
          ) : null}
          {activeTab === 'channels' ? (
            <ChannelsPanel channels={channels} locations={locations} onSave={(channel) => updateChannelMutation.mutate(channel)} updating={updateChannelMutation.isPending} />
          ) : null}
          {activeTab === 'validation' ? <ValidationPanel /> : null}
        </>
      ) : null}
    </section>
  )
}

function LocationSelector({ locations, onChange, value }: { locations: MultisiteLocationSummaryResponse[]; onChange: (value: string) => void; value: string }) {
  return (
    <Card className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-slate-500">Filtro operacional</p>
        <h2 className="mt-1 text-xl font-semibold text-slate-950">Sucursal activa de trabajo</h2>
        <p className="mt-1 text-sm text-slate-500">Usa este filtro para revisar catalogo, horarios, permisos y metricas por sede.</p>
      </div>
      <select className="h-11 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-semibold text-slate-700 outline-none" value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">Todas las sedes</option>
        {locations.map((location) => (
          <option key={location.locationId} value={location.locationId}>{location.locationName}</option>
        ))}
      </select>
    </Card>
  )
}

function TabBar({ activeTab, onChange }: { activeTab: ActiveTab; onChange: (tab: ActiveTab) => void }) {
  const tabs: { label: string; value: ActiveTab }[] = [
    { label: 'Resumen', value: 'summary' },
    { label: 'Catalogo y stock', value: 'catalog' },
    { label: 'Profesionales y horarios', value: 'professionals' },
    { label: 'Permisos por sede', value: 'permissions' },
    { label: 'WhatsApp por sede', value: 'channels' },
    { label: 'Validacion', value: 'validation' },
  ]
  return (
    <div className="grid gap-2 md:grid-cols-3 xl:grid-cols-6">
      {tabs.map((tab) => (
        <button
          className={[
            'rounded-[16px] border px-4 py-3 text-sm font-semibold transition',
            activeTab === tab.value ? 'border-blue-200 bg-blue-50 text-blue-900' : 'border-[var(--color-border)] bg-white text-slate-600 hover:border-blue-200',
          ].join(' ')}
          key={tab.value}
          onClick={() => onChange(tab.value)}
          type="button"
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

function SummaryPanel({ locations }: { locations: MultisiteLocationSummaryResponse[] }) {
  return (
    <div className="grid gap-4 xl:grid-cols-3">
      {locations.map((location) => (
        <Card key={location.locationId} className="space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">{location.locationCode}</p>
              <h3 className="mt-1 text-xl font-semibold text-slate-950">{location.locationName}</h3>
            </div>
            <StatusBadge label={location.active ? 'Activa' : 'Inactiva'} tone={location.active ? 'success' : 'neutral'} />
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <Metric label="Conversaciones" value={location.conversations} />
            <Metric label="Prospectos" value={location.leads} />
            <Metric label="Citas" value={location.bookings} />
            <Metric label="Pedidos" value={location.orders} />
            <Metric label="Productos stock" value={location.productsWithStock} />
            <Metric label="Profesionales" value={location.professionals} />
          </div>
        </Card>
      ))}
    </div>
  )
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-[16px] border border-[var(--color-border)] bg-slate-50 p-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-slate-950">{value}</p>
    </div>
  )
}

function CatalogPanel({ catalogForm, catalogItems, catalogOptions, locations, onChange, onSubmit, saving }: {
  catalogForm: CatalogForm
  catalogItems: MultisiteCatalogAvailabilityResponse[]
  catalogOptions: MultisiteCatalogAvailabilityResponse[]
  locations: MultisiteLocationSummaryResponse[]
  onChange: (form: CatalogForm) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  saving: boolean
}) {
  return (
    <div className="grid gap-4 xl:grid-cols-[380px_minmax(0,1fr)]">
      <Card>
        <h2 className="text-lg font-semibold text-slate-950">Disponibilidad por sede</h2>
        <p className="mt-1 text-sm text-slate-500">Activa servicios/productos por sucursal y registra stock por ubicacion fisica.</p>
        <form className="mt-5 space-y-4" onSubmit={onSubmit}>
          <label className="block text-sm font-medium text-[#23385F]">Producto o servicio</label>
          <select className="h-12 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm" value={catalogForm.productServiceId} onChange={(event) => onChange({ ...catalogForm, productServiceId: event.target.value })}>
            <option value="">Seleccionar item</option>
            {catalogOptions.map((item) => <option key={item.itemId} value={item.itemId}>{item.name}</option>)}
          </select>
          <label className="block text-sm font-medium text-[#23385F]">Sede</label>
          <select className="h-12 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm" value={catalogForm.locationId} onChange={(event) => onChange({ ...catalogForm, locationId: event.target.value })}>
            <option value="">Seleccionar sede</option>
            {locations.map((location) => <option key={location.locationId} value={location.locationId}>{location.locationName}</option>)}
          </select>
          <Input label="Precio especifico de sede" type="number" value={catalogForm.priceOverride} onChange={(event) => onChange({ ...catalogForm, priceOverride: event.target.value })} placeholder="Opcional" />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Stock" type="number" value={catalogForm.stockQuantity} onChange={(event) => onChange({ ...catalogForm, stockQuantity: event.target.value })} />
            <Input label="Stock minimo" type="number" value={catalogForm.stockMinimum} onChange={(event) => onChange({ ...catalogForm, stockMinimum: event.target.value })} />
          </div>
          <Button loading={saving} type="submit" fullWidth>Guardar disponibilidad</Button>
        </form>
      </Card>
      <Card className="overflow-hidden p-0">
        <TableHeader title="Catalogo por sede" description="Servicios/productos disponibles, precio por sucursal y stock operativo." />
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-[var(--color-border)] text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.16em] text-slate-500">
              <tr><th className="px-4 py-3">Item</th><th className="px-4 py-3">Sede</th><th className="px-4 py-3">Estado</th><th className="px-4 py-3">Precio</th><th className="px-4 py-3">Stock</th></tr>
            </thead>
            <tbody className="divide-y divide-[var(--color-border)]">
              {catalogItems.map((item) => (
                <tr key={`${item.itemId}-${item.locationId}`}>
                  <td className="px-4 py-3 font-semibold text-slate-900">{item.name}<p className="text-xs font-normal text-slate-500">{item.type}</p></td>
                  <td className="px-4 py-3">{item.locationName}</td>
                  <td className="px-4 py-3"><StatusBadge label={item.available ? 'Disponible' : 'No disponible'} tone={item.available ? 'success' : 'neutral'} /></td>
                  <td className="px-4 py-3">${Number(item.priceOverride ?? item.basePrice).toLocaleString('es-CL')}</td>
                  <td className="px-4 py-3">{item.stockQuantity ?? '—'} / min {item.stockMinimum ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}

function ProfessionalsPanel({ locations, professionals, scheduleForm, schedules, saving, onChange, onSubmit }: {
  locations: MultisiteLocationSummaryResponse[]
  professionals: MultisiteProfessionalResponse[]
  scheduleForm: ScheduleForm
  schedules: ProfessionalScheduleResponse[]
  saving: boolean
  onChange: (form: ScheduleForm) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  return (
    <div className="grid gap-4 xl:grid-cols-[380px_minmax(0,1fr)]">
      <Card>
        <h2 className="text-lg font-semibold text-slate-950">Horario por sede</h2>
        <p className="mt-1 text-sm text-slate-500">Asocia profesional, sucursal y horario para validar disponibilidad real.</p>
        <form className="mt-5 space-y-4" onSubmit={onSubmit}>
          <label className="block text-sm font-medium text-[#23385F]">Profesional</label>
          <select className="h-12 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm" value={scheduleForm.professionalId} onChange={(event) => onChange({ ...scheduleForm, professionalId: event.target.value })}>
            <option value="">Seleccionar profesional</option>
            {professionals.map((professional) => <option key={professional.professionalId} value={professional.professionalId}>{professional.fullName}</option>)}
          </select>
          <label className="block text-sm font-medium text-[#23385F]">Sede</label>
          <select className="h-12 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm" value={scheduleForm.locationId} onChange={(event) => onChange({ ...scheduleForm, locationId: event.target.value })}>
            <option value="">Seleccionar sede</option>
            {locations.map((location) => <option key={location.locationId} value={location.locationId}>{location.locationName}</option>)}
          </select>
          <div className="grid grid-cols-3 gap-3">
            <Input label="Dia" type="number" min={1} max={7} value={scheduleForm.dayOfWeek} onChange={(event) => onChange({ ...scheduleForm, dayOfWeek: event.target.value })} />
            <Input label="Inicio" type="time" value={scheduleForm.startTime} onChange={(event) => onChange({ ...scheduleForm, startTime: event.target.value })} />
            <Input label="Fin" type="time" value={scheduleForm.endTime} onChange={(event) => onChange({ ...scheduleForm, endTime: event.target.value })} />
          </div>
          <Button loading={saving} type="submit" fullWidth>Guardar horario</Button>
        </form>
      </Card>
      <div className="space-y-4">
        <Card className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {professionals.map((professional) => (
            <div key={professional.professionalId} className="rounded-[18px] border border-[var(--color-border)] p-4">
              <p className="font-semibold text-slate-950">{professional.fullName}</p>
              <p className="mt-1 text-xs text-slate-500">{professional.specialty ?? 'Sin especialidad'}</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {professional.locations.map((location) => <StatusBadge key={location.locationId} label={location.locationName} tone={location.active ? 'info' : 'neutral'} />)}
              </div>
            </div>
          ))}
        </Card>
        <ScheduleTable schedules={schedules} />
      </div>
    </div>
  )
}

function ScheduleTable({ schedules }: { schedules: ProfessionalScheduleResponse[] }) {
  return (
    <Card className="overflow-hidden p-0">
      <TableHeader title="Horarios registrados" description="La agenda debe validar sede, profesional, dia y rango horario antes de confirmar." />
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-[var(--color-border)] text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.16em] text-slate-500"><tr><th className="px-4 py-3">Profesional</th><th className="px-4 py-3">Sede</th><th className="px-4 py-3">Dia</th><th className="px-4 py-3">Horario</th><th className="px-4 py-3">Estado</th></tr></thead>
          <tbody className="divide-y divide-[var(--color-border)]">
            {schedules.map((schedule) => <tr key={schedule.id}><td className="px-4 py-3 font-semibold">{schedule.professionalName}</td><td className="px-4 py-3">{schedule.locationName}</td><td className="px-4 py-3">{schedule.dayOfWeek}</td><td className="px-4 py-3">{schedule.startTime} - {schedule.endTime}</td><td className="px-4 py-3"><StatusBadge label={schedule.active ? 'Activo' : 'Inactivo'} tone={schedule.active ? 'success' : 'neutral'} /></td></tr>)}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

function PermissionsPanel({ access, onToggle, updating }: { access: UserLocationAccessResponse[]; onToggle: (access: UserLocationAccessResponse) => void; updating: boolean }) {
  return (
    <Card className="overflow-hidden p-0">
      <TableHeader title="Usuarios y permisos por sede" description="Controla acceso operativo para evitar que un usuario vea sedes ajenas." />
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-[var(--color-border)] text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.16em] text-slate-500"><tr><th className="px-4 py-3">Usuario</th><th className="px-4 py-3">Sede</th><th className="px-4 py-3">Rol</th><th className="px-4 py-3">Conversaciones</th><th className="px-4 py-3">Agenda</th><th className="px-4 py-3">Pedidos</th><th className="px-4 py-3">Accion</th></tr></thead>
          <tbody className="divide-y divide-[var(--color-border)]">
            {access.map((item) => <tr key={`${item.userId}-${item.locationId}`}><td className="px-4 py-3 font-semibold">{item.userName}<p className="text-xs font-normal text-slate-500">{item.email}</p></td><td className="px-4 py-3">{item.locationName}</td><td className="px-4 py-3">{item.roleScope}</td><td className="px-4 py-3"><BooleanLabel value={item.canViewConversations} /></td><td className="px-4 py-3"><BooleanLabel value={item.canManageBookings} /></td><td className="px-4 py-3"><BooleanLabel value={item.canManageOrders} /></td><td className="px-4 py-3"><Button disabled={updating} size="sm" variant="secondary" onClick={() => onToggle(item)}>Alternar conversaciones</Button></td></tr>)}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

function ChannelsPanel({ channels, locations, onSave, updating }: { channels: MultisiteChannelResponse[]; locations: MultisiteLocationSummaryResponse[]; onSave: (channel: MultisiteChannelResponse) => void; updating: boolean }) {
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  return (
    <Card className="overflow-hidden p-0">
      <TableHeader title="WhatsApp por sede" description="Permite canal centralizado o canal especifico por sucursal." />
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-[var(--color-border)] text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.16em] text-slate-500"><tr><th className="px-4 py-3">Canal</th><th className="px-4 py-3">Estado</th><th className="px-4 py-3">Telefono</th><th className="px-4 py-3">Sede</th><th className="px-4 py-3">Modo</th><th className="px-4 py-3">Accion</th></tr></thead>
          <tbody className="divide-y divide-[var(--color-border)]">
            {channels.map((channel) => {
              const selected = drafts[channel.channelId] ?? channel.locationId ?? ''
              return (
                <tr key={channel.channelId}>
                  <td className="px-4 py-3 font-semibold">{channel.channelType}<p className="text-xs font-normal text-slate-500">{channel.providerName}</p></td>
                  <td className="px-4 py-3"><StatusBadge label={channel.status} tone={channel.status === 'CONNECTED' ? 'success' : 'neutral'} /></td>
                  <td className="px-4 py-3">{channel.phoneNumber ?? 'Sin numero'}</td>
                  <td className="px-4 py-3"><select className="h-10 rounded-[12px] border border-[var(--color-border)] px-3 text-sm" value={selected} onChange={(event) => setDrafts((current) => ({ ...current, [channel.channelId]: event.target.value }))}><option value="">Canal centralizado</option>{locations.map((location) => <option key={location.locationId} value={location.locationId}>{location.locationName}</option>)}</select></td>
                  <td className="px-4 py-3">{selected ? 'LOCATION_SPECIFIC' : 'CENTRALIZED'}</td>
                  <td className="px-4 py-3"><Button disabled={updating} size="sm" onClick={() => onSave({ ...channel, locationId: selected || null })}>Guardar</Button></td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

function ValidationPanel() {
  const items = [
    'No confirmar disponibilidad sin sede, servicio, fecha y hora cuando hay varias sedes activas.',
    'Filtrar agenda, pedidos, prospectos, catalogo y reportes por locationId.',
    'Validar que el profesional atiende en la sede antes de crear o reagendar cita.',
    'Validar stock por sede antes de confirmar pedidos con productos.',
    'Aplicar permisos por sede para conversaciones, agenda, pedidos, catalogo y reportes.',
    'Mantener conversaciones sin sede hasta que el cliente requiera una operacion fisica.',
    'Permitir canal WhatsApp centralizado o canal por sede sin mezclar contactos.',
  ]
  return <Card><h2 className="text-lg font-semibold text-slate-950">Checklist operacional multisede</h2><div className="mt-4 grid gap-3 md:grid-cols-2">{items.map((item) => <div key={item} className="rounded-[16px] border border-emerald-100 bg-emerald-50 p-4 text-sm leading-6 text-emerald-900">✓ {item}</div>)}</div></Card>
}

function TableHeader({ description, title }: { description: string; title: string }) {
  return <div className="border-b border-[var(--color-border)] p-5"><h2 className="text-lg font-semibold text-slate-950">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p></div>
}

function BooleanLabel({ value }: { value: boolean }) {
  return <StatusBadge label={value ? 'Si' : 'No'} tone={value ? 'success' : 'neutral'} />
}
