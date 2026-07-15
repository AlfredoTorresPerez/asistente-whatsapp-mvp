import { zodResolver } from '@hookform/resolvers/zod'
import dayjs from 'dayjs'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { getBookingDetailRequest, updateBookingRequest } from '../../../services/api/bookingsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { BusinessLocationSelect } from '../BusinessLocationSelect'
import { bookingStatusOptions } from '../bookingOptions'

const schema = z.object({
  subject: z.string().trim().min(1, 'Ingresa el asunto.').max(160),
  startsAt: z.string().min(1, 'Selecciona fecha y hora.'),
  durationMinutes: z.coerce.number().min(15).max(720),
  status: z.string().min(1, 'Selecciona un estado.'),
  locationId: z.string().optional(),
  location: z.string().trim().max(160),
  notes: z.string().trim().max(2000),
})

type FormValues = z.infer<typeof schema>
type FormInput = z.input<typeof schema>

export function EditAppointmentPage() {
  const { appointmentId } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      subject: '',
      startsAt: '',
      durationMinutes: 60,
      status: 'PENDIENTE_CONFIRMACION',
      locationId: '',
      location: '',
      notes: '',
    },
  })

  const bookingQuery = useQuery({
    queryKey: ['bookings', 'detail', appointmentId],
    queryFn: () => getBookingDetailRequest(appointmentId ?? ''),
    enabled: Boolean(appointmentId),
  })

  useEffect(() => {
    if (!bookingQuery.data) {
      return
    }

    reset({
      subject: bookingQuery.data.subject,
      startsAt: dayjs(bookingQuery.data.startsAt).format('YYYY-MM-DDTHH:mm'),
      durationMinutes: bookingQuery.data.durationMinutes,
      status: bookingQuery.data.status,
      locationId: bookingQuery.data.locationId ?? '',
      location: bookingQuery.data.location ?? '',
      notes: bookingQuery.data.notes ?? '',
    })
  }, [bookingQuery.data, reset])

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const updateMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }

      return updateBookingRequest(appointmentId, {
        subject: values.subject,
        startsAt: dayjs(values.startsAt).toISOString(),
        durationMinutes: values.durationMinutes,
        status: values.status,
        locationId: values.locationId || undefined,
        location: values.location || undefined,
        notes: values.notes || undefined,
      })
    },
    onSuccess: (booking) => {
      showToast({
        title: 'Cita actualizada',
        description: 'Los cambios ya se reflejan en la agenda.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo actualizar la cita',
        description: 'Revisa los datos e inténtalo nuevamente.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await updateMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button
            onClick={() =>
              navigate(appointmentId ? `/appointments/${appointmentId}` : '/appointments')
            }
            variant="secondary"
          >
            Volver al detalle
          </Button>
        }
        description="Actualiza el estado, horario y notas operativas de una cita existente."
        eyebrow="Editar cita"
        title="Editar cita"
      />

      {bookingQuery.isPending ? (
        <LoadingState message="Cargando la cita para editarla." variant="detail" />
      ) : bookingQuery.isError || !bookingQuery.data ? (
        <ErrorState
          description="No pudimos cargar la cita seleccionada para edición."
          onRetry={() => void bookingQuery.refetch()}
          title="No fue posible abrir la cita"
        />
      ) : (
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

            <div className="grid gap-5 md:grid-cols-2">
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

            <Textarea error={errors.notes?.message} label="Notas" rows={6} {...register('notes')} />

            <div className="flex flex-wrap justify-end gap-3">
              <Button
                onClick={() => navigate(`/appointments/${bookingQuery.data.id}`)}
                variant="secondary"
              >
                Cancelar
              </Button>
              <Button
                disabled={!isOnline}
                loading={updateMutation.isPending || isSubmitting}
                type="submit"
              >
                Guardar cambios
              </Button>
            </div>
          </form>
        </Card>
      )}
    </section>
  )
}
