import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { ApiClientError } from '../../../services/api/httpClient'
import {
  createPublicLandingBookingRequest,
  getPublicLandingAvailabilityRequest,
  getPublicLandingCategoriesRequest,
  getPublicLandingCustomerInfoRequest,
  getPublicLandingServiceBranchesRequest,
  getPublicLandingServicesByCategoryRequest,
} from '../../../services/api/bookingsApi'
import type {
  AgendaAvailabilityRequest,
  AgendaSlotResponse,
  CreatePublicBookingRequest,
  PublicCategoryResponse,
  PublicCustomerInfoResponse,
  PublicServiceBranchResponse,
  PublicServiceItemResponse,
} from '../../../services/api/types'

dayjs.extend(customParseFormat)

function formatTime(value: string) {
  return dayjs(value).format('HH:mm')
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

const STEP_LABELS = ['Categoria', 'Servicio', 'Sucursal', 'Fecha y hora', 'Tus datos', 'Resumen']
const CHILEAN_MOBILE_PHONE_PATTERN = /^569\d{7,8}$/

const CATEGORY_COLORS = [
  'bg-teal-100 text-teal-700',
  'bg-blue-100 text-blue-700',
  'bg-rose-100 text-rose-700',
  'bg-amber-100 text-amber-700',
  'bg-purple-100 text-purple-700',
  'bg-green-100 text-green-700',
  'bg-cyan-100 text-cyan-700',
  'bg-pink-100 text-pink-700',
]

function CategoryIcon({ name, index }: { name: string; index: number }) {
  const colorClass = CATEGORY_COLORS[index % CATEGORY_COLORS.length]
  return (
    <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${colorClass} text-lg font-bold`}>
      {name.charAt(0).toUpperCase()}
    </div>
  )
}

export function CreatePublicBookingPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const [step, setStep] = useState(0)
  const [selectedCategory, setSelectedCategory] = useState<PublicCategoryResponse | null>(null)
  const [selectedService, setSelectedService] = useState<PublicServiceItemResponse | null>(null)
  const [selectedBranch, setSelectedBranch] = useState<PublicServiceBranchResponse | null>(null)
  const [selectedDate, setSelectedDate] = useState('')
  const [dateError, setDateError] = useState('')
  const [phoneError, setPhoneError] = useState('')
  const [selectedSlot, setSelectedSlot] = useState<AgendaSlotResponse | null>(null)
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [customerEmail, setCustomerEmail] = useState('')

  const customerInfoQuery = useQuery({
    queryKey: ['public-landing-customer-info', token],
    queryFn: () => getPublicLandingCustomerInfoRequest(token!),
    enabled: Boolean(token),
    retry: false,
  })

  useEffect(() => {
    if (customerInfoQuery.data) {
      const info = customerInfoQuery.data
      if (info.customerName) setCustomerName(info.customerName)
      if (info.customerPhone) {
        const phone = info.customerPhone.trim().replace(/^\+/, '')
        setCustomerPhone(CHILEAN_MOBILE_PHONE_PATTERN.test(phone) ? phone : '')
      }
      if (info.customerEmail) setCustomerEmail(info.customerEmail)
    }
  }, [customerInfoQuery.data])

  const categoriesQuery = useQuery({
    queryKey: ['public-landing-categories'],
    queryFn: getPublicLandingCategoriesRequest,
    retry: false,
  })

  const servicesQuery = useQuery({
    queryKey: ['public-landing-services', selectedCategory?.code],
    queryFn: () => getPublicLandingServicesByCategoryRequest(selectedCategory!.code),
    enabled: Boolean(selectedCategory),
    retry: false,
  })

  const branchesQuery = useQuery({
    queryKey: ['public-landing-branches', selectedService?.id],
    queryFn: () => getPublicLandingServiceBranchesRequest(selectedService!.id),
    enabled: Boolean(selectedService),
    retry: false,
  })

  useEffect(() => {
    if (customerInfoQuery.data?.lastLocationId && branchesQuery.data) {
      const matched = branchesQuery.data.find((b) => b.id === customerInfoQuery.data.lastLocationId)
      if (matched) {
        setSelectedBranch(matched)
      }
    }
  }, [branchesQuery.data, customerInfoQuery.data])

  const parsedSelectedDate = dayjs(selectedDate, 'DD/MM/YYYY', true)
  const selectedDateIso = selectedDate && parsedSelectedDate.isValid()
    ? parsedSelectedDate.format('YYYY-MM-DD')
    : ''

  const availabilityPayload: AgendaAvailabilityRequest | null =
    selectedBranch && selectedService && selectedDateIso
      ? {
          locationId: selectedBranch.id,
          serviceId: selectedService.id,
          date: selectedDateIso,
          maxSlots: 20,
        }
      : null

  const availabilityQuery = useQuery({
    queryKey: ['public-landing-availability', availabilityPayload],
    queryFn: () => getPublicLandingAvailabilityRequest(availabilityPayload!),
    enabled: Boolean(availabilityPayload),
    retry: false,
  })

  const createBookingMutation = useMutation({
    mutationFn: () => {
      const payload: CreatePublicBookingRequest = {
        locationId: selectedBranch!.id,
        serviceId: selectedService!.id,
        professionalId: selectedSlot?.professionalId ?? undefined,
        roomId: selectedSlot?.roomId ?? undefined,
        startsAt: selectedSlot!.startsAt,
        customerName: customerName.trim(),
        customerPhone: customerPhone.trim().replace(/^\+/, ''),
        customerEmail: customerEmail.trim() || undefined,
      }
      return createPublicLandingBookingRequest(payload)
    },
    onSuccess: () => {
      setStep(6)
    },
  })

  const availableSlots = availabilityQuery.data?.slots.filter((slot) => slot.available) ?? []

  const canProceedFromStep = (currentStep: number) => {
    switch (currentStep) {
      case 0:
        return Boolean(selectedCategory)
      case 1:
        return Boolean(selectedService)
      case 2:
        return Boolean(selectedBranch)
      case 3:
        return Boolean(selectedDateIso) && Boolean(selectedSlot) && !dateError
      case 4: {
        const phone = customerPhone.trim().replace(/^\+/, '')
        return customerName.trim().length >= 2 && CHILEAN_MOBILE_PHONE_PATTERN.test(phone)
      }
      case 5:
        return !createBookingMutation.isPending
      default:
        return false
    }
  }

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(45,212,191,0.22),_transparent_38%),linear-gradient(180deg,_#f8fafc_0%,_#eef6f8_100%)] px-4 py-10">
      <section className="mx-auto max-w-2xl space-y-6">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">Asistente WhatsApp Centro Estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Reserva tu hora</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">Selecciona el servicio, sucursal y horario que prefieras.</p>
          {token && customerInfoQuery.isSuccess && customerInfoQuery.data?.customerName ? (
            <div className="mt-3 rounded-xl bg-teal-50 px-4 py-3 text-sm text-teal-700">
              Bienvenido de nuevo, {customerInfoQuery.data.customerName} &#x1f60a; Tus datos ya estan cargados.
            </div>
          ) : null}
        </div>

        {step < 6 && (
          <div className="flex items-center justify-center gap-1 sm:gap-2">
            {STEP_LABELS.map((label, i) => (
              <div key={label} className="flex items-center gap-1 sm:gap-2">
                <div
                  className={`flex h-7 w-7 sm:h-8 sm:w-8 items-center justify-center rounded-full text-xs font-bold ${
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
                {i < STEP_LABELS.length - 1 && <div className="h-px w-2 sm:w-4 bg-slate-200" />}
              </div>
            ))}
          </div>
        )}

        {step === 0 && (
          <div className="space-y-4">
            {categoriesQuery.isPending ? (
              <Card className="text-center">
                <p className="text-sm font-medium text-slate-700">Cargando categorias...</p>
              </Card>
            ) : categoriesQuery.isError ? (
              <Card className="border-rose-200 bg-rose-50 text-center">
                <p className="text-sm text-rose-700">
                  {getErrorMessage(categoriesQuery.error, 'No se pudieron cargar las categorias.')}
                </p>
              </Card>
            ) : !categoriesQuery.data || categoriesQuery.data.length === 0 ? (
              <Card className="text-center">
                <p className="text-sm text-slate-500">No hay categorias disponibles.</p>
              </Card>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2">
                {categoriesQuery.data.map((cat, i) => (
                  <button
                    key={cat.id}
                    onClick={() => setSelectedCategory(cat)}
                    className={`rounded-2xl border p-5 text-left shadow-sm transition hover:shadow-md ${
                      selectedCategory?.id === cat.id
                        ? 'border-teal-400 bg-teal-50'
                        : 'border-slate-200 bg-white hover:border-teal-300'
                    }`}
                    type="button"
                  >
                    <div className="flex items-center gap-3">
                      <CategoryIcon name={cat.name} index={i} />
                      <div>
                        <h3 className="font-semibold text-slate-950">{cat.name}</h3>
                        {cat.description ? (
                          <p className="mt-1 text-sm text-slate-500">{cat.description}</p>
                        ) : null}
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            )}
            <div className="flex justify-end">
              <Button disabled={!canProceedFromStep(0)} onClick={() => setStep(1)}>
                Continuar
              </Button>
            </div>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Button onClick={() => setStep(0)} variant="secondary" size="sm">
                ← Volver
              </Button>
              <p className="text-sm text-slate-500">{selectedCategory?.name}</p>
            </div>
            {servicesQuery.isPending ? (
              <Card className="text-center">
                <p className="text-sm font-medium text-slate-700">Cargando servicios...</p>
              </Card>
            ) : servicesQuery.isError ? (
              <Card className="border-rose-200 bg-rose-50 text-center">
                <p className="text-sm text-rose-700">
                  {getErrorMessage(servicesQuery.error, 'No se pudieron cargar los servicios.')}
                </p>
              </Card>
            ) : !servicesQuery.data || servicesQuery.data.length === 0 ? (
              <Card className="text-center">
                <p className="text-sm text-slate-500">No hay servicios en esta categoria.</p>
              </Card>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2">
                {(() => {
                  const bookable = servicesQuery.data.filter((s) => !s.requiresPriorEvaluation)
                  const needsEvaluation = servicesQuery.data.filter((s) => s.requiresPriorEvaluation)
                  return (
                    <>
                      {bookable.map((svc) => (
                        <button
                          key={svc.id}
                          onClick={() => setSelectedService(svc)}
                          className={`rounded-2xl border p-5 text-left shadow-sm transition hover:shadow-md ${
                            selectedService?.id === svc.id
                              ? 'border-teal-400 bg-teal-50'
                              : 'border-slate-200 bg-white hover:border-teal-300'
                          }`}
                          type="button"
                        >
                          <h3 className="font-semibold text-slate-950">{svc.name}</h3>
                          {svc.description ? (
                            <p className="mt-1 text-sm text-slate-500">{svc.description}</p>
                          ) : null}
                          <p className="mt-2 text-sm text-slate-500">{svc.durationMinutes} min</p>
                          <p className="mt-1 text-sm font-semibold text-teal-700">
                            ${Number(svc.priceBase).toLocaleString('es-CL')}
                          </p>
                        </button>
                      ))}
                      {needsEvaluation.length > 0 && (
                        <div className="col-span-full mt-2">
                          <p className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                            Requieren evaluacion previa
                          </p>
                          <div className="mt-2 grid gap-4 sm:grid-cols-2">
                            {needsEvaluation.map((svc) => (
                              <div
                                key={svc.id}
                                className="rounded-2xl border border-slate-200 bg-slate-50 p-5 text-left opacity-60"
                              >
                                <h3 className="font-semibold text-slate-950">{svc.name}</h3>
                                {svc.description ? (
                                  <p className="mt-1 text-sm text-slate-500">{svc.description}</p>
                                ) : null}
                                <p className="mt-2 text-sm text-slate-500">{svc.durationMinutes} min</p>
                                <p className="mt-1 text-sm font-semibold text-teal-700">
                                  ${Number(svc.priceBase).toLocaleString('es-CL')}
                                </p>
                                <p className="mt-2 text-xs text-amber-600">
                                  Requiere evaluacion presencial previa.
                                </p>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </>
                  )
                })()}
              </div>
            )}
            <div className="flex justify-between">
              <Button onClick={() => setStep(0)} variant="secondary">
                Atras
              </Button>
              <Button disabled={!canProceedFromStep(1)} onClick={() => setStep(2)}>
                Continuar
              </Button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Button onClick={() => setStep(1)} variant="secondary" size="sm">
                ← Volver
              </Button>
              <p className="text-sm text-slate-500">{selectedService?.name}</p>
            </div>
            {branchesQuery.isPending ? (
              <Card className="text-center">
                <p className="text-sm font-medium text-slate-700">Cargando sucursales...</p>
              </Card>
            ) : branchesQuery.isError ? (
              <Card className="border-rose-200 bg-rose-50 text-center">
                <p className="text-sm text-rose-700">
                  {getErrorMessage(branchesQuery.error, 'No se pudieron cargar las sucursales.')}
                </p>
              </Card>
            ) : !branchesQuery.data || branchesQuery.data.length === 0 ? (
              <Card className="text-center">
                <p className="text-sm text-slate-500">No hay sucursales disponibles para este servicio.</p>
              </Card>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2">
                {branchesQuery.data.map((branch) => (
                  <button
                    key={branch.id}
                    onClick={() => setSelectedBranch(branch)}
                    className={`rounded-2xl border p-5 text-left shadow-sm transition hover:shadow-md ${
                      selectedBranch?.id === branch.id
                        ? 'border-teal-400 bg-teal-50'
                        : 'border-slate-200 bg-white hover:border-teal-300'
                    }`}
                    type="button"
                  >
                    <h3 className="font-semibold text-slate-950">{branch.name}</h3>
                    {branch.address ? (
                      <p className="mt-1 text-sm text-slate-500">{branch.address}</p>
                    ) : null}
                    {branch.city ? (
                      <p className="text-sm text-slate-500">{branch.city}</p>
                    ) : null}
                  </button>
                ))}
              </div>
            )}
            <div className="flex justify-between">
              <Button onClick={() => setStep(1)} variant="secondary">
                Atras
              </Button>
              <Button disabled={!canProceedFromStep(2)} onClick={() => setStep(3)}>
                Continuar
              </Button>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Button onClick={() => setStep(2)} variant="secondary" size="sm">
                ← Volver
              </Button>
              <p className="text-sm text-slate-500">{selectedBranch?.name}</p>
            </div>
            <Card className="space-y-4">
              <div>
                <label className="mb-2.5 block text-sm font-medium text-[#23385F]">Selecciona una fecha</label>
                <div className="relative">
                  <div
                    className={`flex h-12 w-full cursor-pointer items-center rounded-[14px] border bg-white px-4 text-sm transition ${
                      dateError
                        ? 'border-red-300 ring-4 ring-red-100/60'
                        : 'border-[var(--color-border)]'
                    } ${selectedDate ? 'text-[var(--color-text)]' : 'text-slate-400'}`}
                    onClick={() => {
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
                            setSelectedSlot(null)
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
                    }}
                  >
                    <svg className="mr-2 h-5 w-5 text-slate-400" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    {selectedDate || 'Seleccionar fecha'}
                  </div>
                </div>
                {dateError ? (
                  <span className="mt-2 block text-sm text-red-700">{dateError}</span>
                ) : null}
              </div>
              {selectedDate ? (
                <div>
                  <div className="flex items-center justify-between gap-3">
                    <h4 className="text-sm font-semibold text-slate-900">Horarios disponibles</h4>
                    <Button
                      loading={availabilityQuery.isFetching}
                      onClick={() => {
                        if (availabilityPayload) {
                          availabilityQuery.refetch()
                        }
                      }}
                      type="button"
                      variant="secondary"
                      size="sm"
                    >
                      Actualizar
                    </Button>
                  </div>
                  <div className="mt-3 grid gap-3 sm:grid-cols-2">
                    {availabilityQuery.isPending ? (
                      <p className="col-span-full text-sm text-slate-500">Cargando disponibilidad...</p>
                    ) : availabilityQuery.isError ? (
                      <p className="col-span-full rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                        {getErrorMessage(
                          availabilityQuery.error,
                          'No fue posible consultar horarios disponibles.',
                        )}
                      </p>
                    ) : availableSlots.length === 0 ? (
                      <p className="col-span-full text-sm text-slate-500">
                        No hay horarios disponibles para la fecha seleccionada.
                      </p>
                    ) : (
                      availableSlots.map((slot) => (
                        <button
                          key={`${slot.startsAt}-${slot.professionalId ?? 'sin-prof'}-${slot.roomId ?? 'sin-room'}`}
                          onClick={() => setSelectedSlot(slot)}
                          className={`rounded-2xl border p-4 text-left shadow-sm transition hover:shadow-md ${
                            selectedSlot?.startsAt === slot.startsAt
                              ? 'border-teal-400 bg-teal-50'
                              : 'border-slate-200 bg-white hover:border-teal-300'
                          }`}
                          type="button"
                        >
                          <strong className="text-slate-950">
                            {formatTime(slot.startsAt)} - {formatTime(slot.endsAt)}
                          </strong>
                          <p className="mt-2 text-sm text-slate-600">
                            {slot.professionalName ?? 'Profesional por asignar'}
                          </p>
                          <p className="text-sm text-slate-500">
                            {slot.roomName ?? 'Sin cabina requerida'}
                          </p>
                        </button>
                      ))
                    )}
                  </div>
                </div>
              ) : (
                <p className="text-sm text-slate-500">Elige una fecha para ver los horarios disponibles.</p>
              )}
            </Card>
            <div className="flex justify-between">
              <Button onClick={() => setStep(2)} variant="secondary">
                Atras
              </Button>
              <Button disabled={!canProceedFromStep(3)} onClick={() => setStep(4)}>
                Continuar
              </Button>
            </div>
          </div>
        )}

        {step === 4 && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Button onClick={() => setStep(3)} variant="secondary" size="sm">
                ← Volver
              </Button>
            </div>
            <Card className="space-y-4">
              <Input
                label="Nombre completo"
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder="Ej: Maria Perez"
                value={customerName}
              />
              <div>
                <Input
                  label="Telefono"
                  onChange={(e) => {
                    setCustomerPhone(e.target.value)
                    setPhoneError('')
                  }}
                  onBlur={() => {
                    const phone = customerPhone.trim().replace(/^\+/, '')
                    if (customerPhone.trim() && !CHILEAN_MOBILE_PHONE_PATTERN.test(phone)) {
                      setPhoneError('Formato invalido. Debe ser 569XXXXXXXX (ej: 56912345678)')
                    }
                  }}
                  placeholder="Ej: 56912345678"
                  value={customerPhone}
                />
                {phoneError ? (
                  <span className="mt-1 block text-sm text-red-700">{phoneError}</span>
                ) : null}
              </div>
              <Input
                label="Email (opcional)"
                onChange={(e) => setCustomerEmail(e.target.value)}
                placeholder="Ej: maria@correo.cl"
                type="email"
                value={customerEmail}
              />
            </Card>
            <div className="flex justify-between">
              <Button onClick={() => setStep(3)} variant="secondary">
                Atras
              </Button>
              <Button disabled={!canProceedFromStep(4)} onClick={() => setStep(5)}>
                Continuar
              </Button>
            </div>
          </div>
        )}

        {step === 5 && (
          <div className="space-y-4">
            <Card className="space-y-6">
              <div className="rounded-2xl bg-teal-50 p-5">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-teal-700">
                  Resumen de tu reserva
                </p>
                <p className="mt-2 text-sm text-teal-800">
                  Revisa los datos antes de confirmar.
                </p>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <Info label="Servicio" value={selectedService?.name ?? ''} />
                <Info label="Duracion" value={`${selectedService?.durationMinutes ?? 0} minutos`} />
                <Info label="Sucursal" value={selectedBranch?.name ?? ''} />
                <Info label="Direccion" value={selectedBranch?.address ?? ''} />
                <Info label="Fecha" value={selectedDate} />
                <Info
                  label="Horario"
                  value={`${formatTime(selectedSlot?.startsAt ?? '')} - ${formatTime(selectedSlot?.endsAt ?? '')}`}
                />
                <Info
                  label="Profesional"
                  value={selectedSlot?.professionalName ?? 'Por asignar'}
                />
                <Info label="Cabina" value={selectedSlot?.roomName ?? 'No requerida'} />
                <Info label="Nombre" value={customerName} />
                <Info label="Telefono" value={customerPhone} />
                {customerEmail ? <Info label="Email" value={customerEmail} /> : null}
              </div>

              {createBookingMutation.isError ? (
                <div className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                  <p>{getErrorMessage(
                    createBookingMutation.error,
                    'No fue posible crear la reserva. Intenta nuevamente.',
                  )}</p>
                  {createBookingMutation.error instanceof ApiClientError &&
                    Object.keys(createBookingMutation.error.fieldErrors).length > 0 && (
                    <ul className="mt-1 list-inside list-disc">
                      {Object.entries(createBookingMutation.error.fieldErrors).map(([field, msg]) => (
                        <li key={field}>{msg}</li>
                      ))}
                    </ul>
                  )}
                </div>
              ) : null}

              <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-between">
                <Button onClick={() => setStep(4)} variant="secondary">
                  Atras
                </Button>
                <Button
                  disabled={!canProceedFromStep(5)}
                  loading={createBookingMutation.isPending}
                  onClick={() => createBookingMutation.mutate()}
                >
                  {createBookingMutation.isPending ? 'Reservando...' : 'Confirmar reserva'}
                </Button>
              </div>
            </Card>
          </div>
        )}

        {step === 6 ? (
          <Card className="space-y-6 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-teal-100">
              <svg
                className="h-8 w-8 text-teal-600"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <div>
              <h2 className="text-2xl font-semibold text-slate-950">Reserva creada con exito</h2>
              <p className="mt-2 text-sm text-slate-600">
                Tu reserva ha sido registrada. Te enviaremos la confirmacion por WhatsApp.
              </p>
            </div>
            <div className="mx-auto max-w-sm rounded-2xl border border-slate-200 bg-white p-4">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                Codigo de reserva
              </p>
              <p className="mt-1 text-lg font-bold text-teal-700">
                {createBookingMutation.data?.bookingId}
              </p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Info
                label="Servicio"
                value={createBookingMutation.data?.serviceName ?? selectedService?.name ?? ''}
              />
              <Info
                label="Sucursal"
                value={createBookingMutation.data?.locationName ?? selectedBranch?.name ?? ''}
              />
              <Info
                label="Fecha y hora"
                value={dayjs(
                  createBookingMutation.data?.startsAt ?? selectedSlot?.startsAt,
                ).format('DD/MM/YYYY HH:mm')}
              />
              <Info
                label="Cliente"
                value={createBookingMutation.data?.customerName ?? customerName}
              />
            </div>
            <Button
              onClick={() => {
                setStep(0)
                setSelectedCategory(null)
                setSelectedService(null)
                setSelectedBranch(null)
                setSelectedDate('')
                setSelectedSlot(null)
                setCustomerName('')
                setCustomerPhone('')
                setCustomerEmail('')
                createBookingMutation.reset()
              }}
            >
              Nueva reserva
            </Button>
          </Card>
        ) : null}
      </section>
    </main>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-950">{value}</p>
    </div>
  )
}
