import { zodResolver } from '@hookform/resolvers/zod'
import dayjs from 'dayjs'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { createBookingRequest } from '../../../services/api/bookingsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import { BusinessLocationSelect } from '../BusinessLocationSelect'
import { bookingStatusOptions } from '../bookingOptions'

const schema = z.object({
  subject: z.string().trim().min(1, 'Ingresa el asunto.').max(160),
  customerName: z.string().trim().min(1, 'Ingresa el nombre del cliente.').max(160),
  customerPhone: z.string().trim().min(8, 'Ingresa un telefono valido.').max(30),
  customerEmail: z.string().trim().max(255).email('Ingresa un correo valido.').or(z.literal('')),
  startsAt: z.string().min(1, 'Selecciona fecha y hora.'),
  durationMinutes: z.coerce.number().min(15).max(720),
  status: z.string().min(1, 'Selecciona un estado.'),
  locationId: z.string().optional(),
  location: z.string().trim().max(160),
  notes: z.string().trim().max(2000),
})

type FormValues = z.infer<typeof schema>
type FormInput = z.input<typeof schema>

export function NewAppointmentPage() {
  const navigate = useNavigate()
  const { user } = useShellSession()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      subject: '',
      customerName: '',
      customerPhone: '',
      customerEmail: '',
      startsAt: dayjs().add(1, 'day').hour(10).minute(0).format('YYYY-MM-DDTHH:mm'),
      durationMinutes: 60,
      status: 'PENDIENTE_CONFIRMACION',
      locationId: '',
      location: '',
      notes: '',
    },
  })

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const createMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload = {
        subject: values.subject,
        customerName: values.customerName,
        customerPhone: values.customerPhone,
        customerEmail: values.customerEmail || undefined,
        startsAt: dayjs(values.startsAt).toISOString(),
        durationMinutes: values.durationMinutes,
        status: values.status,
        locationId: values.locationId || undefined,
        location: values.location || undefined,
        notes: values.notes || undefined,
        assignedUserId: user?.id,
      }
      return createBookingRequest(payload)
    },
    onSuccess: (booking) => {
      showToast({
        title: 'Cita creada',
        description: 'La cita ya aparece en la agenda y quedo lista para seguimiento.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    },
    onError: (error) => {
      const apiError = error as ApiClientError
      if (apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0) {
        const firstFieldError = Object.values(apiError.fieldErrors)[0]
        showToast({
          title: 'No se pudo crear la cita',
          description: firstFieldError,
          tone: 'error',
        })
      } else {
        showToast({
          title: 'No se pudo crear la cita',
          description: apiError?.message ?? 'Revisa los datos ingresados y vuelve a intentarlo.',
          tone: 'error',
        })
      }
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button onClick={() => navigate('/appointments')} variant="secondary">
            Volver a agenda
          </Button>
        }
        description="Registra una cita manual indicando cliente, fecha, duracion y notas operativas."
        eyebrow="Nueva cita"
        title="Crear cita"
      />

      <Card>
        <form className="space-y-5" onSubmit={onSubmit}>
          <div className="grid gap-5 md:grid-cols-2">
            <Input error={errors.subject?.message} label="Asunto" {...register('subject')} />
            <Select
              error={errors.status?.message}
              label="Estado"
              options={bookingStatusOptions}
              {...register('status')}
            />
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <Input
              error={errors.customerName?.message}
              label="Cliente"
              {...register('customerName')}
            />
            <Input
              error={errors.customerPhone?.message}
              label="Telefono"
              {...register('customerPhone')}
            />
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <Input
              error={errors.customerEmail?.message}
              label="Correo"
              type="email"
              {...register('customerEmail')}
            />
            <BusinessLocationSelect
              error={errors.locationId?.message}
              locations={locationsQuery.data}
              registration={register('locationId')}
            />
            <Input
              error={errors.location?.message}
              label="Ubicacion complementaria"
              placeholder="Sala, box o referencia interna"
              {...register('location')}
            />
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <Input
              error={errors.startsAt?.message}
              label="Fecha y hora"
              type="datetime-local"
              {...register('startsAt')}
            />
            <Input
              error={errors.durationMinutes?.message}
              label="Duracion (minutos)"
              min={15}
              max={720}
              type="number"
              {...register('durationMinutes')}
            />
          </div>

          <Textarea error={errors.notes?.message} label="Notas" rows={6} {...register('notes')} />

          <div className="flex flex-wrap justify-end gap-3">
            <Button onClick={() => navigate('/appointments')} variant="secondary">
              Cancelar
            </Button>
            <Button
              disabled={!isOnline}
              loading={createMutation.isPending || isSubmitting}
              type="submit"
            >
              Crear cita
            </Button>
          </div>
        </form>
      </Card>
    </section>
  )
}
