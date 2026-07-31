import { useQueries } from '@tanstack/react-query'
import { useMemo } from 'react'
import { listAestheticServices } from '../../../services/api/aestheticApi'
import { listAestheticProducts } from '../../../services/api/aestheticApi'
import { listAestheticRules } from '../../../services/api/aestheticApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { listProfessionalsRequest } from '../../../services/api/professionalsApi'
import { listRoomsRequest } from '../../../services/api/roomsApi'
import { getMultisiteSchedulesRequest } from '../../../services/api/multisiteApi'
import { listAssignmentsRequest } from '../../../services/api/assignmentsApi'
import type {
  AestheticServiceResponse,
  BusinessLocationResponse,
  ProfessionalResponse,
  ProfessionalScheduleResponse,
} from '../../../services/api/types'

export type SummaryCard = {
  key: string
  label: string
  count: number
  activeCount: number
  warnings: string[]
  adminLink: string
  adminLabel: string
}

export type ReadinessCheck = {
  key: string
  label: string
  passed: boolean
  detail: string | null
}

function checkServicePrice(service: AestheticServiceResponse): string | null {
  return service.active && service.priceBase <= 0 ? `"${service.name}" sin precio` : null
}

function checkServiceDuration(service: AestheticServiceResponse): string | null {
  return service.active && (!service.durationMinutes || service.durationMinutes <= 0) ? `"${service.name}" sin duración` : null
}

function checkServiceProfessional(service: AestheticServiceResponse, assignments: { serviceId: string; professionalId: string | null }[]): string | null {
  if (!service.active) return null
  const hasProfessional = assignments.some((a) => a.serviceId === service.id && a.professionalId)
  return service.professionalRequired && !hasProfessional ? `"${service.name}" sin profesional asignado` : null
}

function checkServiceRoom(service: AestheticServiceResponse, assignments: { serviceId: string; roomId: string | null }[]): string | null {
  if (!service.active) return null
  const hasRoom = assignments.some((a) => a.serviceId === service.id && a.roomId)
  return service.roomIds.length > 0 && !hasRoom ? `"${service.name}" sin cabina asignada` : null
}

function checkLocationAddress(location: BusinessLocationResponse): string | null {
  return location.active && !location.address ? `"${location.name}" sin dirección` : null
}

function checkLocationSchedule(locationId: string, locationName: string, schedules: ProfessionalScheduleResponse[]): string | null {
  const hasSchedule = schedules.some((s) => s.locationId === locationId)
  return !hasSchedule ? `"${locationName}" sin horario configurado` : null
}

function checkProfessionalsActive(professionals: ProfessionalResponse[], services: AestheticServiceResponse[], assignments: { serviceId: string; professionalId: string | null }[]): string | null {
  const inactiveAssigned = professionals.filter(
    (p) => !p.active && assignments.some((a) => a.professionalId === p.id),
  )
  if (inactiveAssigned.length > 0) {
    return `${inactiveAssigned.length} profesional(es) inactivo(s) asignado(s) a servicios`
  }
  return null
}

function checkServiceWithoutAvailability(service: AestheticServiceResponse, schedules: ProfessionalScheduleResponse[], assignments: { serviceId: string; roomId: string | null }[]): string | null {
  if (!service.active) return null
  const assignedRooms = assignments.filter((a) => a.serviceId === service.id && a.roomId)
  if (assignedRooms.length === 0) return null
  return null
}

export function useBusinessReadiness(userPermissions: string[] = []) {
  const hasCatView = userPermissions.includes('CATALOG_VIEW') || userPermissions.includes('ALL')
  const hasLocView = userPermissions.includes('LOCATIONS_VIEW') || userPermissions.includes('ALL')
  const hasProfView = userPermissions.includes('PROFESSIONAL_VIEW') || userPermissions.includes('ALL')
  const hasRoomView = userPermissions.includes('ROOM_VIEW') || userPermissions.includes('ALL')
  const hasAutoManage = userPermissions.includes('AUTOMATION_MANAGE') || userPermissions.includes('ALL')
  const hasAssignView = userPermissions.includes('ASSIGNMENT_VIEW') || userPermissions.includes('ALL')

  const results = useQueries({
    queries: [
      {
        queryKey: ['business-readiness', 'services'],
        queryFn: () => listAestheticServices({ size: 200 }),
        enabled: hasCatView,
      },
      {
        queryKey: ['business-readiness', 'products'],
        queryFn: () => listAestheticProducts({ size: 200 }),
        enabled: hasCatView,
      },
      {
        queryKey: ['business-readiness', 'rules'],
        queryFn: () => listAestheticRules({ size: 200 }),
        enabled: hasAutoManage,
      },
      {
        queryKey: ['business-readiness', 'locations'],
        queryFn: () => getBusinessLocationsRequest({ activeOnly: false }),
        enabled: hasLocView,
      },
      {
        queryKey: ['business-readiness', 'professionals'],
        queryFn: () => listProfessionalsRequest({ size: 200 }),
        enabled: hasProfView,
      },
      {
        queryKey: ['business-readiness', 'rooms'],
        queryFn: () => listRoomsRequest({ size: 200 }),
        enabled: hasRoomView,
      },
      {
        queryKey: ['business-readiness', 'schedules'],
        queryFn: () => getMultisiteSchedulesRequest(),
        enabled: hasLocView,
      },
      {
        queryKey: ['business-readiness', 'assignments'],
        queryFn: () => listAssignmentsRequest(),
        enabled: hasAssignView,
      },
    ],
  })

  const [
    servicesQuery,
    productsQuery,
    rulesQuery,
    locationsQuery,
    professionalsQuery,
    roomsQuery,
    schedulesQuery,
    assignmentsQuery,
  ] = results

  const services = useMemo(() => servicesQuery.data?.items ?? [], [servicesQuery.data?.items])
  const products = useMemo(() => productsQuery.data?.items ?? [], [productsQuery.data?.items])
  const rules = useMemo(() => rulesQuery.data?.items ?? [], [rulesQuery.data?.items])
  const locations = useMemo(() => {
    const data = locationsQuery.data
    return Array.isArray(data) ? data : []
  }, [locationsQuery.data])
  const professionals = useMemo(() => professionalsQuery.data?.items ?? [], [professionalsQuery.data?.items])
  const rooms = useMemo(() => roomsQuery.data?.items ?? [], [roomsQuery.data?.items])
  const schedules = useMemo(() => {
    const data = schedulesQuery.data
    return Array.isArray(data) ? data : []
  }, [schedulesQuery.data])
  const assignments = useMemo(() => {
    const data = assignmentsQuery.data
    return Array.isArray(data) ? data : []
  }, [assignmentsQuery.data])

  const policyTypes = ['SAFETY', 'AVAILABILITY', 'PAYMENT', 'COMMERCIAL']
  const policies = useMemo(() => rules.filter((r) => policyTypes.includes(r.ruleType)), [rules])

  const summaryCards: SummaryCard[] = useMemo(() => [
    {
      key: 'services',
      label: 'Servicios activos',
      count: services.length,
      activeCount: services.filter((s) => s.active).length,
      warnings: services.filter((s) => s.active && s.priceBase <= 0).map((s) => `"${s.name}" sin precio`),
      adminLink: '/admin/services',
      adminLabel: 'Administrar servicios',
    },
    {
      key: 'products',
      label: 'Productos',
      count: products.length,
      activeCount: products.filter((p) => p.active).length,
      warnings: products.filter((p) => p.active && p.stock <= 0).map((p) => `"${p.name}" sin stock`),
      adminLink: '/catalog',
      adminLabel: 'Administrar productos',
    },
    {
      key: 'promotions',
      label: 'Promociones vigentes',
      count: 0,
      activeCount: 0,
      warnings: [],
      adminLink: '/admin/multisite',
      adminLabel: 'Administrar promociones',
    },
    {
      key: 'locations',
      label: 'Sucursales activas',
      count: locations.length,
      activeCount: locations.filter((l) => l.active).length,
      warnings: locations.filter((l) => l.active && !l.address).map((l) => `"${l.name}" sin dirección`),
      adminLink: '/admin/branches',
      adminLabel: 'Administrar sucursales',
    },
    {
      key: 'professionals',
      label: 'Profesionales disponibles',
      count: professionals.length,
      activeCount: professionals.filter((p) => p.active).length,
      warnings: professionals.filter((p) => !p.active && assignments.some((a) => a.professionalId === p.id)).map((p) => `"${p.fullName}" inactivo asignado`),
      adminLink: '/admin/professionals',
      adminLabel: 'Administrar profesionales',
    },
    {
      key: 'rooms',
      label: 'Cabinas disponibles',
      count: rooms.length,
      activeCount: rooms.filter((r) => r.active).length,
      warnings: [],
      adminLink: '/admin/rooms',
      adminLabel: 'Administrar cabinas',
    },
    {
      key: 'schedules',
      label: 'Horarios configurados',
      count: schedules.length,
      activeCount: schedules.filter((s) => s.active).length,
      warnings: locations.filter((l) => l.active && !schedules.some((s) => s.locationId === l.id)).map((l) => `"${l.name}" sin horario`),
      adminLink: '/admin/multisite',
      adminLabel: 'Administrar horarios',
    },
    {
      key: 'policies',
      label: 'Políticas activas',
      count: policies.length,
      activeCount: policies.filter((p) => p.active).length,
      warnings: policies.filter((p) => p.active && !p.description).map((p) => `"${p.name}" incompleta`),
      adminLink: '/automation-rules',
      adminLabel: 'Administrar políticas',
    },
  ], [services, products, locations, professionals, rooms, schedules, policies, assignments])

  const readinessChecks: ReadinessCheck[] = useMemo(() => {
    const checks: ReadinessCheck[] = []

    const servicesNoPrice = services.filter((s) => checkServicePrice(s))
    checks.push({
      key: 'service-price',
      label: 'Servicios sin precio',
      passed: servicesNoPrice.length === 0,
      detail: servicesNoPrice.length > 0 ? `Faltan precios en ${servicesNoPrice.length} servicio(s).` : null,
    })

    const servicesNoDuration = services.filter((s) => checkServiceDuration(s))
    checks.push({
      key: 'service-duration',
      label: 'Servicios sin duración',
      passed: servicesNoDuration.length === 0,
      detail: servicesNoDuration.length > 0 ? `Faltan duración en ${servicesNoDuration.length} servicio(s).` : null,
    })

    const servicesNoProf = services.filter((s) => checkServiceProfessional(s, assignments))
    checks.push({
      key: 'service-professional',
      label: 'Servicios sin profesional asignado',
      passed: servicesNoProf.length === 0,
      detail: servicesNoProf.length > 0 ? `${servicesNoProf.length} servicio(s) requieren profesional.` : null,
    })

    const servicesNoRoom = services.filter((s) => checkServiceRoom(s, assignments))
    checks.push({
      key: 'service-room',
      label: 'Servicios sin cabina asignada',
      passed: servicesNoRoom.length === 0,
      detail: servicesNoRoom.length > 0 ? `${servicesNoRoom.length} servicio(s) sin cabina.` : null,
    })

    const locationsNoAddress = locations.filter((l) => checkLocationAddress(l))
    checks.push({
      key: 'location-address',
      label: 'Sucursales sin dirección',
      passed: locationsNoAddress.length === 0,
      detail: locationsNoAddress.length > 0 ? `${locationsNoAddress.length} sucursal(es) sin dirección.` : null,
    })

    const locationsNoSchedule = locations.filter((l) => l.active && checkLocationSchedule(l.id, l.name, schedules))
    checks.push({
      key: 'location-schedule',
      label: 'Sucursales sin horario',
      passed: locationsNoSchedule.length === 0,
      detail: locationsNoSchedule.length > 0 ? `${locationsNoSchedule.length} sucursal(es) sin horario.` : null,
    })

    const expiredPromos = 0
    checks.push({
      key: 'promotions-expired',
      label: 'Promociones vencidas',
      passed: expiredPromos === 0,
      detail: null,
    })

    const incompletePolicies = policies.filter((p) => p.active && !p.description)
    checks.push({
      key: 'policy-complete',
      label: 'Políticas incompletas',
      passed: incompletePolicies.length === 0,
      detail: incompletePolicies.length > 0 ? `${incompletePolicies.length} política(s) incompleta(s).` : null,
    })

    const inactiveAssigned = professionals.filter(
      (p) => !p.active && assignments.some((a) => a.professionalId === p.id),
    )
    checks.push({
      key: 'professional-active',
      label: 'Profesionales inactivos asignados',
      passed: inactiveAssigned.length === 0,
      detail: inactiveAssigned.length > 0 ? `${inactiveAssigned.length} profesional(es) inactivo(s) asignado(s).` : null,
    })

    const activeWithDurationAndRoom = services.filter((s) => s.active && (s.durationMinutes ?? 0) > 0 && assignments.some((a) => a.serviceId === s.id && a.roomId))
    const noAvailability = services.filter((s) => {
      if (!s.active) return false
      const hasRoom = assignments.some((a) => a.serviceId === s.id && a.roomId)
      if (!hasRoom) return false
      return !schedules.some((sc) => {
        const roomLocation = rooms.find((r) => r.id === assignments.find((a) => a.serviceId === s.id && a.roomId)?.roomId)?.locationId
        return roomLocation && sc.locationId === roomLocation
      })
    })
    checks.push({
      key: 'service-availability',
      label: 'Servicios visibles sin disponibilidad',
      passed: noAvailability.length === 0,
      detail: noAvailability.length > 0 ? `${noAvailability.length} servicio(s) sin disponibilidad configurada.` : null,
    })

    return checks
  }, [services, locations, professionals, rooms, schedules, policies, assignments])

  const passedCount = useMemo(() => readinessChecks.filter((c) => c.passed).length, [readinessChecks])

  const isLoading = results.some((r) => r.isLoading)

  return {
    summaryCards,
    readinessChecks,
    passedCount,
    totalChecks: readinessChecks.length,
    isLoading,
  }
}
