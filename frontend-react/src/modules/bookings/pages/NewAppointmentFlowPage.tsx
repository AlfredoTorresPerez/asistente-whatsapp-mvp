import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { listAestheticServices } from '../../../services/api/aestheticApi'
import { searchCustomersRequest } from '../../../services/api/bookingsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import {
  createTemporaryAgendaBookingRequest,
  getAgendaAvailabilityRequest,
  getAgendaFilterOptionsRequest,
} from '../../../services/api/completeAgendaApi'
import { ApiClientError } from '../../../services/api/httpClient'
import {
  SLOT_PERIOD_LABELS,
  compareSlots,
  getSlotTimePeriod,
  slotIdentity,
  type SlotTimePeriod,
} from '../utils/slotTimePeriod'
import type {
  AgendaAvailabilityRequest,
  AgendaSlotResponse,
  AestheticServiceResponse,
  BusinessLocationResponse,
  CustomerSearchResponse,
} from '../../../services/api/types'

dayjs.extend(customParseFormat)

const STEP_LABELS = ['Cliente', 'Servicio y sucursal', 'Fecha y hora', 'Resumen']
const CHILEAN_MOBILE_PHONE_PATTERN = /^\+569\d{8}$/
const SOURCE_OPTIONS = [
  { label: 'Agenda interna', value: 'AGENDA' },
  { label: 'WhatsApp', value: 'WHATSAPP' },
  { label: 'Telefono', value: 'TELEFONO' },
  { label: 'Presencial', value: 'PRESENCIAL' },
  { label: 'Sitio web', value: 'WEB' },
]

function formatTime(value?: string | null) {
  return value ? dayjs(value).format('HH:mm') : ''
}

function formatMoney(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-'
  }
  return new Intl.NumberFormat('es-CL', {
    currency: 'CLP',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

function normalizeChilePhoneInput(value: string) {
  const digitsOnly = value.replace(/\D/g, '')
  const digits = digitsOnly.startsWith('00') ? digitsOnly.slice(2) : digitsOnly
  if (digits.startsWith('569') && digits.length === 11) {
    return `+${digits}`
  }
  if (digits.startsWith('56') && digits.length === 11) {
    return `+${digits}`
  }
  if (digits.startsWith('9') && digits.length === 9) {
    return `+56${digits}`
  }
  if (digits.length === 8) {
    return `+569${digits}`
  }
  return null
}

function customerPhone(customer: CustomerSearchResponse | null, newCustomerPhone: string) {
  return customer?.normalizedPhone ?? normalizeChilePhoneInput(newCustomerPhone) ?? ''
}

function customerEmail(customer: CustomerSearchResponse | null, newCustomerEmail: string) {
  return customer?.email ?? newCustomerEmail.trim()
}

function sameSlot(a: AgendaSlotResponse | null, b: AgendaSlotResponse) {
  return a !== null && slotIdentity(a) === slotIdentity(b)
}

export function NewAppointmentFlowPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const [step, setStep] = useState(0)
  const [searchPhone, setSearchPhone] = useState('')
  const [searchName, setSearchName] = useState('')
  const [searchEmail, setSearchEmail] = useState('')
  const [showCreateCustomer, setShowCreateCustomer] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')
  const [newCustomerEmail, setNewCustomerEmail] = useState('')
  const [communicationsConsent, setCommunicationsConsent] = useState(false)
  const [phoneError, setPhoneError] = useState('')
  const [slotMessage, setSlotMessage] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerSearchResponse | null>(null)
  const [selectedService, setSelectedService] = useState<AestheticServiceResponse | undefined>()
  const [selectedLocation, setSelectedLocation] = useState<BusinessLocationResponse | undefined>()
  const [selectedProfessionalId, setSelectedProfessionalId] = useState('')
  const [selectedRoomId, setSelectedRoomId] = useState('')
  const [selectedDate, setSelectedDate] = useState('')
  const [dateError, setDateError] = useState('')
  const [selectedPeriod, setSelectedPeriod] = useState<SlotTimePeriod | null>(null)
  const [selectedSlot, setSelectedSlot] = useState<AgendaSlotResponse | null>(null)
  const [origin, setOrigin] = useState('AGENDA')
  const [notes, setNotes] = useState('')
  const [sendWhatsApp, setSendWhatsApp] = useState(false)
  const [isCreating, setIsCreating] = useState(false)

  const normalizedNewCustomerPhone = useMemo(
    () => normalizeChilePhoneInput(newCustomerPhone),
    [newCustomerPhone],
  )

  useEffect(() => {
    if (selectedCustomer) {
      setNewCustomerName(selectedCustomer.displayName)
      setNewCustomerPhone(selectedCustomer.normalizedPhone)
      setNewCustomerEmail(selectedCustomer.email ?? '')
      setCommunicationsConsent(false)
    }
  }, [selectedCustomer])

  const customerSearchQuery = useQuery({
    queryKey: ['customer-search', searchPhone, searchName, searchEmail],
    queryFn: () =>
      searchCustomersRequest({
        email: searchEmail || undefined,
        name: searchName || undefined,
        phone: searchPhone || undefined,
      }),
    enabled: false,
    retry: false,
  })

  const duplicateCustomerQuery = useQuery({
    queryKey: ['customer-duplicate-phone', normalizedNewCustomerPhone],
    queryFn: () => searchCustomersRequest({ phone: normalizedNewCustomerPhone ?? undefined }),
    enabled: showCreateCustomer && Boolean(normalizedNewCustomerPhone),
    retry: false,
  })

  const duplicateCustomer = useMemo(() => {
    if (!normalizedNewCustomerPhone) return null
    return (
      duplicateCustomerQuery.data?.find(
        (customer) => customer.normalizedPhone === normalizedNewCustomerPhone,
      ) ?? null
    )
  }, [duplicateCustomerQuery.data, normalizedNewCustomerPhone])

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const servicesQuery = useQuery({
    queryKey: ['aesthetic-services', 'active'],
    queryFn: () => listAestheticServices({ active: true }),
  })

  const filterOptionsQuery = useQuery({
    queryKey: ['agenda-filter-options', selectedLocation?.id],
    queryFn: () => getAgendaFilterOptionsRequest({ locationId: selectedLocation?.id }),
    enabled: Boolean(selectedLocation),
    retry: false,
  })

  const parsedSelectedDate = dayjs(selectedDate, 'DD/MM/YYYY', true)
  const selectedDateIso =
    selectedDate && parsedSelectedDate.isValid() ? parsedSelectedDate.format('YYYY-MM-DD') : ''

  const availabilityPayload: AgendaAvailabilityRequest | null =
    selectedLocation && selectedService && selectedDateIso
      ? {
          date: selectedDateIso,
          locationId: selectedLocation.id,
          maxSlots: 40,
          professionalId: selectedProfessionalId || undefined,
          roomId: selectedRoomId || undefined,
          serviceId: selectedService.id,
        }
      : null

  const availabilityQuery = useQuery({
    queryKey: ['agenda-availability', availabilityPayload],
    queryFn: () => getAgendaAvailabilityRequest(availabilityPayload!),
    enabled: Boolean(availabilityPayload),
    retry: false,
  })

  const availableSlots = useMemo(() => {
    const slots = availabilityQuery.data?.slots.filter((slot) => slot.available) ?? []
    return slots.slice().sort(compareSlots)
  }, [availabilityQuery.data])

  const professionalOptions = useMemo(() => {
    const options = filterOptionsQuery.data?.professionals ?? []
    return [
      { label: 'Cualquier profesional disponible', value: '' },
      ...options.filter((option) => option.active).map((option) => ({ label: option.name, value: option.id })),
    ]
  }, [filterOptionsQuery.data])

  const roomOptions = useMemo(() => {
    const options = filterOptionsQuery.data?.rooms ?? []
    return [
      { label: 'Cualquier cabina compatible', value: '' },
      ...options.filter((option) => option.active).map((option) => ({ label: option.name, value: option.id })),
    ]
  }, [filterOptionsQuery.data])

  const morningSlots = availableSlots.filter((slot) => getSlotTimePeriod(slot.startsAt) === 'MORNING')
  const afternoonSlots = availableSlots.filter((slot) => getSlotTimePeriod(slot.startsAt) === 'AFTERNOON')

  const periodSlots =
    selectedPeriod === 'MORNING'
      ? morningSlots
      : selectedPeriod === 'AFTERNOON'
        ? afternoonSlots
        : []

  const selectedPrice = Number(selectedService?.priceBase ?? 0)
  const requiresDeposit = Boolean(availabilityQuery.data?.requiresDeposit)

  useEffect(() => {
    setSelectedSlot(null)
    setSelectedPeriod(null)
    setSlotMessage('')
  }, [selectedLocation?.id, selectedService?.id, selectedProfessionalId, selectedRoomId, selectedDateIso])

  useEffect(() => {
    const slots = availabilityQuery.data?.slots.filter((slot) => slot.available) ?? []
    setSelectedSlot((current) => {
      if (!current) return current
      const stillAvailable = slots.some((slot) => slotIdentity(slot) === slotIdentity(current))
      if (!stillAvailable) {
        setSlotMessage('El horario seleccionado ya no esta disponible. Elige una alternativa actualizada.')
      }
      return stillAvailable ? current : null
    })
    setSelectedPeriod((current) => {
      if (!current) return current
      const periodHasSlots = slots.some((slot) => getSlotTimePeriod(slot.startsAt) === current)
      return periodHasSlots ? current : null
    })
  }, [availabilityQuery.data])

  const searchCustomer = () => {
    if (!searchPhone.trim() && !searchName.trim() && !searchEmail.trim()) return
    customerSearchQuery.refetch()
  }

  const refreshAvailability = () => {
    if (!availabilityPayload || availabilityQuery.isFetching) return
    availabilityQuery.refetch()
  }

  const selectPeriod = (period: SlotTimePeriod) => {
    setSelectedPeriod(period)
    setSelectedSlot(null)
  }

  const openDatePicker = () => {
    const input = document.createElement('input')
    input.type = 'date'
    input.min = dayjs().format('YYYY-MM-DD')
    input.style.position = 'fixed'
    input.style.opacity = '0'
    input.style.pointerEvents = 'none'
    input.style.left = '-9999px'
    const handler = (e: Event) => {
      const iso = (e.target as HTMLInputElement).value
      if (iso) {
        const d = dayjs(iso)
        if (d.isBefore(dayjs().startOf('day'))) {
          setDateError('La fecha debe ser hoy o posterior')
        } else {
          setSelectedDate(d.format('DD/MM/YYYY'))
          setDateError('')
        }
      }
      input.remove()
    }
    input.addEventListener('change', handler, { once: true })
    input.addEventListener('blur', () => input.remove(), { once: true })
    document.body.appendChild(input)
    if (typeof input.showPicker === 'function') {
      input.showPicker()
    } else {
      input.click()
    }
  }

  const canCreateNewCustomer =
    showCreateCustomer &&
    newCustomerName.trim().length >= 2 &&
    Boolean(normalizedNewCustomerPhone) &&
    !duplicateCustomer

  const canProceedFromStep = (currentStep: number) => {
    switch (currentStep) {
      case 0:
        return Boolean(selectedCustomer || canCreateNewCustomer)
      case 1:
        return Boolean(selectedService && selectedLocation && origin)
      case 2:
        return Boolean(selectedDateIso && selectedSlot && !dateError && !availabilityQuery.isFetching)
      case 3:
        return !isCreating
      default:
        return false
    }
  }

  const handlePhoneBlur = () => {
    if (!newCustomerPhone.trim()) {
      setPhoneError('')
      return
    }
    if (!normalizedNewCustomerPhone || !CHILEAN_MOBILE_PHONE_PATTERN.test(normalizedNewCustomerPhone)) {
      setPhoneError('Ingresa un telefono movil chileno valido.')
      return
    }
    setPhoneError('')
  }

  const handleConfirmClick = async () => {
    if (!canProceedFromStep(3) || !selectedLocation || !selectedService || !selectedSlot) return
    setIsCreating(true)
    try {
      const phone = customerPhone(selectedCustomer, newCustomerPhone)
      const booking = await createTemporaryAgendaBookingRequest({
        communicationsConsent,
        customerEmail: customerEmail(selectedCustomer, newCustomerEmail) || undefined,
        customerId: selectedCustomer?.id,
        customerName: selectedCustomer?.displayName ?? newCustomerName.trim(),
        customerPhone: phone,
        generateConfirmationLink: true,
        locationId: selectedLocation.id,
        notes: notes.trim() || undefined,
        professionalId: (selectedSlot.professionalId ?? selectedProfessionalId) || undefined,
        roomId: (selectedSlot.roomId ?? selectedRoomId) || undefined,
        sendWhatsApp: sendWhatsApp && communicationsConsent,
        serviceId: selectedService.id,
        sourceChannel: origin,
        startsAt: dayjs(selectedSlot.startsAt).toISOString(),
      })
      showToast({
        title: 'Cita creada',
        description: 'La cita fue creada correctamente.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    } catch (error) {
      if (availabilityPayload) {
        availabilityQuery.refetch()
      }
      const apiError = error as ApiClientError
      const firstFieldError =
        apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
          ? Object.values(apiError.fieldErrors)[0]
          : null
      showToast({
        title: 'No se pudo crear la cita',
        description:
          firstFieldError ??
          apiError?.message ??
          'La disponibilidad pudo cambiar. Revisa los horarios e intenta nuevamente.',
        tone: 'error',
      })
    } finally {
      setIsCreating(false)
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button onClick={() => navigate('/appointments')} variant="secondary">
            Volver a agenda
          </Button>
        }
        description="Crea una cita paso a paso con cliente, servicio, sucursal y horario disponible."
        eyebrow="Nueva cita"
        title="Crear cita"
      />

      <div className="flex items-center justify-center gap-1 sm:gap-2">
        {STEP_LABELS.map((label, i) => (
          <div key={label} className="flex items-center gap-1 sm:gap-2">
            <div
              className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold sm:h-8 sm:w-8 ${
                i === step
                  ? 'bg-teal-600 text-white'
                  : i < step
                    ? 'bg-teal-100 text-teal-700'
                    : 'bg-slate-100 text-slate-400'
              }`}
            >
              {i < step ? '✓' : i + 1}
            </div>
            <span
              className={`hidden text-xs sm:inline ${
                i === step ? 'font-semibold text-slate-900' : 'text-slate-400'
              }`}
            >
              {label}
            </span>
            {i < STEP_LABELS.length - 1 && <div className="h-px w-2 bg-slate-200 sm:w-4" />}
          </div>
        ))}
      </div>

      {step === 0 && (
        <Card className="space-y-5">
          <h3 className="text-sm font-semibold text-slate-900">Cliente</h3>
          <div className="grid gap-4 lg:grid-cols-3">
            <Input
              label="Telefono"
              placeholder="Ingresa telefono"
              value={searchPhone}
              onChange={(e) => setSearchPhone(e.target.value)}
            />
            <Input
              label="Nombre"
              placeholder="Nombre del cliente"
              value={searchName}
              onChange={(e) => setSearchName(e.target.value)}
            />
            <Input
              label="Correo electronico"
              placeholder="Correo del cliente"
              type="email"
              value={searchEmail}
              onChange={(e) => setSearchEmail(e.target.value)}
            />
          </div>
          <div className="flex flex-wrap gap-3">
            <Button onClick={searchCustomer} disabled={!searchPhone.trim() && !searchName.trim() && !searchEmail.trim()}>
              Buscar
            </Button>
            <Button
              onClick={() => {
                setShowCreateCustomer(true)
                setSelectedCustomer(null)
              }}
              variant="secondary"
              size="sm"
            >
              Crear cliente
            </Button>
          </div>

          {customerSearchQuery.isPending && <p className="text-sm text-slate-500">Buscando clientes...</p>}
          {customerSearchQuery.isError && (
            <p className="text-sm text-rose-700">
              {getErrorMessage(customerSearchQuery.error, 'No se pudieron buscar clientes.')}
            </p>
          )}
          {customerSearchQuery.data && customerSearchQuery.data.length === 0 && (
            <p className="text-sm text-slate-500">No se encontraron clientes.</p>
          )}
          {customerSearchQuery.data && customerSearchQuery.data.length > 0 && (
            <div className="grid gap-3 lg:grid-cols-2">
              {customerSearchQuery.data.map((customer) => (
                <button
                  key={customer.id}
                  type="button"
                  onClick={() => {
                    setSelectedCustomer(customer)
                    setShowCreateCustomer(false)
                  }}
                  className={`rounded-lg border p-4 text-left transition ${
                    selectedCustomer?.id === customer.id
                      ? 'border-teal-400 bg-teal-50'
                      : 'border-slate-200 bg-white hover:border-teal-300'
                  }`}
                >
                  <p className="font-semibold text-slate-950">{customer.displayName}</p>
                  <p className="text-sm text-slate-500">{customer.email ?? 'Sin correo registrado'}</p>
                  <p className="text-sm text-slate-500">{customer.normalizedPhone}</p>
                </button>
              ))}
            </div>
          )}

          {showCreateCustomer && (
            <div className="space-y-4 rounded-lg border border-slate-200 bg-slate-50 p-4">
              <h4 className="text-sm font-semibold text-slate-900">Nuevo cliente</h4>
              <div className="grid gap-4 lg:grid-cols-2">
                <Input
                  label="Nombre completo"
                  placeholder="Nombre y apellido"
                  value={newCustomerName}
                  onChange={(e) => setNewCustomerName(e.target.value)}
                />
                <Input
                  error={phoneError}
                  label="Telefono"
                  placeholder="Telefono chileno"
                  value={newCustomerPhone}
                  onBlur={handlePhoneBlur}
                  onChange={(e) => {
                    setNewCustomerPhone(e.target.value)
                    setPhoneError('')
                  }}
                />
                <Input
                  label="Correo electronico opcional"
                  placeholder="Correo del cliente"
                  type="email"
                  value={newCustomerEmail}
                  onChange={(e) => setNewCustomerEmail(e.target.value)}
                />
                <label className="flex min-h-12 items-center gap-3 rounded-lg border border-slate-200 bg-white px-4 text-sm text-slate-700">
                  <input
                    checked={communicationsConsent}
                    className="h-4 w-4 rounded border-slate-300 text-teal-600"
                    onChange={(e) => {
                      setCommunicationsConsent(e.target.checked)
                      if (!e.target.checked) setSendWhatsApp(false)
                    }}
                    type="checkbox"
                  />
                  Autoriza recibir comunicaciones de la cita
                </label>
              </div>
              {normalizedNewCustomerPhone && !duplicateCustomer ? (
                <p className="text-sm text-slate-500">Telefono normalizado: {normalizedNewCustomerPhone}</p>
              ) : null}
              {duplicateCustomer ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                  <p className="font-semibold">Ya existe un cliente con este telefono.</p>
                  <p>Selecciona el cliente existente para evitar duplicados.</p>
                  <Button
                    className="mt-3"
                    size="sm"
                    onClick={() => {
                      setSelectedCustomer(duplicateCustomer)
                      setShowCreateCustomer(false)
                    }}
                  >
                    Usar cliente existente
                  </Button>
                </div>
              ) : null}
            </div>
          )}

          <div className="flex justify-end">
            <Button disabled={!canProceedFromStep(0)} onClick={() => setStep(1)}>
              Continuar
            </Button>
          </div>
        </Card>
      )}

      {step === 1 && (
        <Card className="space-y-5">
          <StepBack onClick={() => setStep(0)} text={selectedCustomer?.displayName ?? newCustomerName} />
          <div className="grid gap-4 lg:grid-cols-2">
            <Select
              label="Sucursal"
              options={[
                { label: 'Selecciona una sucursal', value: '' },
                ...(locationsQuery.data ?? []).map((location) => ({ label: location.name, value: location.id })),
              ]}
              value={selectedLocation?.id ?? ''}
              onChange={(e) => {
                const location = locationsQuery.data?.find((item) => item.id === e.target.value)
                setSelectedLocation(location)
                setSelectedProfessionalId('')
                setSelectedRoomId('')
              }}
            />
            <Select
              label="Servicio"
              options={[
                { label: 'Selecciona un servicio', value: '' },
                ...(servicesQuery.data?.items ?? []).map((service) => ({ label: service.name, value: service.id })),
              ]}
              value={selectedService?.id ?? ''}
              onChange={(e) => {
                const service = servicesQuery.data?.items.find((item) => item.id === e.target.value)
                setSelectedService(service)
              }}
            />
            <Select
              disabled={!selectedLocation || filterOptionsQuery.isPending}
              label="Profesional"
              options={professionalOptions}
              value={selectedProfessionalId}
              onChange={(e) => setSelectedProfessionalId(e.target.value)}
            />
            <Select
              disabled={!selectedLocation || filterOptionsQuery.isPending}
              label="Cabina"
              options={roomOptions}
              value={selectedRoomId}
              onChange={(e) => setSelectedRoomId(e.target.value)}
            />
            <Select
              label="Origen de la cita"
              options={SOURCE_OPTIONS}
              value={origin}
              onChange={(e) => setOrigin(e.target.value)}
            />
          </div>

          {selectedService ? (
            <div className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
              <Info label="Servicio" value={selectedService.name} />
              <Info label="Duracion" value={`${selectedService.durationMinutes} minutos`} />
              <Info label="Precio" value={formatMoney(Number(selectedService.priceBase))} />
              <Info
                label="Abono"
                value={availabilityQuery.data?.requiresDeposit ? 'Requerido segun politica' : 'No requerido'}
              />
            </div>
          ) : null}

          {locationsQuery.isError || servicesQuery.isError || filterOptionsQuery.isError ? (
            <p className="text-sm text-rose-700">No se pudieron cargar todas las opciones necesarias.</p>
          ) : null}

          <Textarea
            label="Observaciones administrativas"
            placeholder="Indicaciones internas para esta cita"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />

          <div className="flex justify-between">
            <Button onClick={() => setStep(0)} variant="secondary">
              Atras
            </Button>
            <Button disabled={!canProceedFromStep(1)} onClick={() => setStep(2)}>
              Continuar
            </Button>
          </div>
        </Card>
      )}

      {step === 2 && (
        <Card className="space-y-5">
          <StepBack
            onClick={() => setStep(1)}
            text={`${selectedService?.name ?? ''} · ${selectedLocation?.name ?? ''}`}
          />
          <div>
            <label className="mb-2.5 block text-sm font-medium text-[#23385F]">Fecha</label>
            <div
              role="button"
              tabIndex={0}
              aria-label="Seleccionar fecha"
              className={`flex h-12 w-full cursor-pointer items-center rounded-[14px] border bg-white px-4 text-sm transition ${
                dateError ? 'border-red-300 ring-4 ring-red-100/60' : 'border-[var(--color-border)]'
              } ${selectedDate ? 'text-[var(--color-text)]' : 'text-slate-400'}`}
              onClick={openDatePicker}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  openDatePicker()
                }
              }}
            >
              {selectedDate || 'Seleccionar fecha'}
            </div>
            {dateError ? <span className="mt-2 block text-sm text-red-700">{dateError}</span> : null}
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Select
              disabled={!selectedLocation || filterOptionsQuery.isPending}
              label="Profesional"
              options={professionalOptions}
              value={selectedProfessionalId}
              onChange={(e) => setSelectedProfessionalId(e.target.value)}
            />
            <Select
              disabled={!selectedLocation || filterOptionsQuery.isPending}
              label="Cabina"
              options={roomOptions}
              value={selectedRoomId}
              onChange={(e) => setSelectedRoomId(e.target.value)}
            />
          </div>

          {slotMessage ? (
            <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              {slotMessage}
            </div>
          ) : null}

          {selectedDate ? (
            <div>
              <div className="flex items-center justify-between gap-3">
                <h4 className="text-sm font-semibold text-slate-900">Horarios disponibles</h4>
                <Button
                  loading={availabilityQuery.isFetching}
                  disabled={availabilityQuery.isFetching}
                  onClick={refreshAvailability}
                  type="button"
                  variant="secondary"
                  size="sm"
                >
                  Actualizar
                </Button>
              </div>
              {availabilityQuery.isPending ? (
                <p className="mt-3 text-sm text-slate-500" role="status">
                  Cargando disponibilidad...
                </p>
              ) : availabilityQuery.isError ? (
                <div className="mt-3 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700" role="alert">
                  {getErrorMessage(
                    availabilityQuery.error,
                    'No fue posible consultar los horarios disponibles. Intenta nuevamente.',
                  )}
                </div>
              ) : availableSlots.length === 0 ? (
                <p className="mt-3 text-sm text-slate-500">
                  No encontramos horarios disponibles para esta fecha.
                </p>
              ) : (
                <div className="mt-3 space-y-4">
                  <fieldset disabled={availabilityQuery.isFetching}>
                    <legend className="text-sm font-medium text-slate-700">
                      Selecciona el tramo horario
                    </legend>
                    <div className="mt-2 grid gap-3 sm:grid-cols-2">
                      <PeriodButton
                        period="MORNING"
                        label={SLOT_PERIOD_LABELS.MORNING}
                        count={morningSlots.length}
                        selected={selectedPeriod === 'MORNING'}
                        onSelect={selectPeriod}
                      />
                      <PeriodButton
                        period="AFTERNOON"
                        label={SLOT_PERIOD_LABELS.AFTERNOON}
                        count={afternoonSlots.length}
                        selected={selectedPeriod === 'AFTERNOON'}
                        onSelect={selectPeriod}
                      />
                    </div>
                  </fieldset>

                  {selectedPeriod ? (
                    <div aria-live="polite">
                      <h5 className="text-sm font-semibold text-slate-900">
                        Horarios disponibles en la {SLOT_PERIOD_LABELS[selectedPeriod].toLowerCase()}
                      </h5>
                      <div className="mt-2 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                        {periodSlots.map((slot) => {
                          const isSelected = sameSlot(selectedSlot, slot)
                          return (
                            <button
                              key={slotIdentity(slot)}
                              type="button"
                              onClick={() => {
                                setSelectedSlot(slot)
                                setSlotMessage('')
                              }}
                              aria-pressed={isSelected}
                              aria-label={slotAccessibleLabel(slot, isSelected)}
                              className={`rounded-lg border p-4 text-left shadow-sm transition hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 ${
                                isSelected
                                  ? 'border-teal-400 bg-teal-50'
                                  : 'border-slate-200 bg-white hover:border-teal-300'
                              }`}
                            >
                              <strong className="block text-xl font-bold text-slate-950">
                                {formatTime(slot.startsAt)}
                              </strong>
                              <p className="mt-1 text-sm font-medium text-slate-700">
                                {slot.professionalName ?? 'Profesional por asignar'}
                              </p>
                              <p className="text-sm text-slate-500">Hasta {formatTime(slot.endsAt)}</p>
                              <p className="mt-1 text-xs text-slate-500">
                                {slot.roomName ? `${slot.roomName} · ${slot.locationName}` : slot.locationName}
                              </p>
                            </button>
                          )
                        })}
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-slate-500">Selecciona manana o tarde para ver alternativas.</p>
                  )}
                </div>
              )}
            </div>
          ) : (
            <p className="text-sm text-slate-500">Elige una fecha para consultar disponibilidad.</p>
          )}

          <div className="flex justify-between">
            <Button onClick={() => setStep(1)} variant="secondary">
              Atras
            </Button>
            <Button disabled={!canProceedFromStep(2)} onClick={() => setStep(3)}>
              Continuar
            </Button>
          </div>
        </Card>
      )}

      {step === 3 && (
        <Card className="space-y-6">
          <div className="rounded-lg bg-teal-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-teal-700">
              Resumen de la cita
            </p>
            <p className="mt-2 text-sm text-teal-800">Revisa los datos antes de crear la cita.</p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Info label="Cliente" value={selectedCustomer?.displayName ?? newCustomerName} />
            <Info label="Telefono" value={customerPhone(selectedCustomer, newCustomerPhone)} />
            <Info label="Correo electronico" value={customerEmail(selectedCustomer, newCustomerEmail) || '-'} />
            <Info label="Sucursal" value={selectedLocation?.name ?? ''} />
            <Info label="Servicio" value={selectedService?.name ?? ''} />
            <Info label="Profesional" value={selectedSlot?.professionalName ?? 'Por asignar'} />
            <Info label="Cabina" value={selectedSlot?.roomName ?? 'No requerida'} />
            <Info label="Fecha" value={selectedDate} />
            <Info label="Hora de inicio" value={formatTime(selectedSlot?.startsAt)} />
            <Info label="Hora de termino" value={formatTime(selectedSlot?.endsAt)} />
            <Info label="Duracion" value={`${selectedService?.durationMinutes ?? 0} minutos`} />
            <Info label="Precio" value={formatMoney(selectedPrice)} />
            <Info label="Abono" value={requiresDeposit ? 'Requerido segun politica' : 'No requerido'} />
            <Info label="Saldo" value={requiresDeposit ? 'Pendiente segun pago registrado' : formatMoney(selectedPrice)} />
            <Info
              label="Politica de cancelacion"
              value={selectedService?.cancellationRules || 'Politica vigente del negocio'}
            />
            <Info label="Canal de origen" value={SOURCE_OPTIONS.find((item) => item.value === origin)?.label ?? ''} />
            <Info
              label="Notificaciones"
              value={
                sendWhatsApp && communicationsConsent
                  ? 'Se enviara confirmacion por WhatsApp'
                  : 'No se enviara confirmacion automatica'
              }
            />
          </div>

          <label className="flex items-center gap-3 rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-700">
            <input
              checked={sendWhatsApp}
              className="h-4 w-4 rounded border-slate-300 text-teal-600"
              disabled={!communicationsConsent}
              onChange={(e) => setSendWhatsApp(e.target.checked)}
              type="checkbox"
            />
            Enviar confirmacion por WhatsApp
          </label>

          <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-between">
            <Button onClick={() => setStep(2)} variant="secondary">
              Atras
            </Button>
            <Button disabled={!canProceedFromStep(3) || !isOnline} loading={isCreating} onClick={handleConfirmClick}>
              {isCreating ? 'Creando...' : 'Crear cita'}
            </Button>
          </div>
        </Card>
      )}
    </section>
  )
}

function StepBack({ onClick, text }: { onClick: () => void; text: string }) {
  return (
    <div className="flex items-center gap-2">
      <Button onClick={onClick} variant="secondary" size="sm">
        Volver
      </Button>
      <p className="truncate text-sm text-slate-500">{text}</p>
    </div>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-950">{value || '-'}</p>
    </div>
  )
}

function PeriodButton({
  period,
  label,
  count,
  selected,
  onSelect,
}: {
  period: SlotTimePeriod
  label: string
  count: number
  selected: boolean
  onSelect: (period: SlotTimePeriod) => void
}) {
  const hasSlots = count > 0
  const accessibleLabel = `${label}, ${hasSlots ? `${count} horarios disponibles` : 'sin horarios disponibles'}.`
  return (
    <button
      type="button"
      disabled={!hasSlots}
      aria-pressed={selected}
      aria-label={accessibleLabel}
      onClick={() => onSelect(period)}
      className={`rounded-lg border px-4 py-3 text-left text-sm font-semibold shadow-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 ${
        !hasSlots
          ? 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400'
          : selected
            ? 'border-teal-400 bg-teal-50 text-teal-900 ring-1 ring-teal-300'
            : 'border-slate-200 bg-white text-slate-700 hover:border-teal-300'
      }`}
    >
      {label}
      <span className={hasSlots ? 'text-slate-500' : 'text-slate-400'}>
        {' '}
        · {hasSlots ? `${count} horarios` : 'Sin horarios'}
      </span>
    </button>
  )
}

function slotAccessibleLabel(slot: AgendaSlotResponse, selected: boolean) {
  const parts = [
    `Hora ${formatTime(slot.startsAt)}`,
    `hasta ${formatTime(slot.endsAt)}`,
    slot.professionalName ?? 'Profesional por asignar',
    slot.roomName ?? 'Sin cabina requerida',
    slot.locationName,
  ]
  if (selected) {
    parts.push('seleccionado')
  }
  return parts.join(', ')
}
