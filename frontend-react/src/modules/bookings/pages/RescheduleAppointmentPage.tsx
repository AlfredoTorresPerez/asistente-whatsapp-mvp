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
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  createBookingRescheduleLinkRequest,
  getBookingDetailRequest,
  rescheduleBookingRequest,
} from '../../../services/api/bookingsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { BusinessLocationSelect } from '../BusinessLocationSelect'

const schema = z.object({
  startsAt: z.string().min(1, 'Selecciona la nueva fecha y hora.'),
  durationMinutes: z.coerce.number().min(15).max(720),
  locationId: z.string().optional(),
  location: z.string().trim().max(160),
  notes: z.string().trim().max(2000),
})

type FormValues = z.infer<typeof schema>
type FormInput = z.input<typeof schema>

export function RescheduleAppointmentPage() {
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
      startsAt: '',
      durationMinutes: 60,
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
      startsAt: dayjs(bookingQuery.data.startsAt).format('YYYY-MM-DDTHH:mm'),
      durationMinutes: bookingQuery.data.durationMinutes,
      locationId: bookingQuery.data.locationId ?? '',
      location: bookingQuery.data.location ?? '',
      notes: bookingQuery.data.notes ?? '',
    })
  }, [bookingQuery.data, reset])

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const rescheduleMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }

      return rescheduleBookingRequest(appointmentId, {
        startsAt: dayjs(values.startsAt).toISOString(),
        durationMinutes: values.durationMinutes,
        locationId: values.locationId || undefined,
        location: values.location || undefined,
        notes: values.notes || undefined,
      })
    },
    onSuccess: (booking) => {
      showToast({
        title: 'Cita reprogramada',
        description: 'La agenda ya refleja el nuevo horario.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo reprogramar la cita',
        description: 'Revisa el nuevo horario e inténtalo nuevamente.',
        tone: 'error',
      })
    },
  })

  const rescheduleLinkMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!appointmentId || !bookingQuery.data) {
        throw new Error('No hay cita seleccionada.')
      }
      const locationId = values.locationId || bookingQuery.data.locationId
      if (!locationId) {
        throw new Error('Selecciona una sucursal antes de enviar el enlace.')
      }
      return createBookingRescheduleLinkRequest(appointmentId, {
        locationId,
        startsAt: dayjs(values.startsAt).toISOString(),
        reason: values.notes || 'Propuesta de reprogramacion enviada desde agenda.',
        expirationMinutes: 720,
        sendWhatsApp: true,
        sendEmail: true,
      })
    },
    onSuccess: () => {
      showToast({
        title: 'Enlace de reprogramacion enviado',
        description: 'El cliente puede confirmar o rechazar la nueva fecha desde la pagina publica.',
        tone: 'success',
      })
      navigate(appointmentId ? `/appointments/${appointmentId}` : '/appointments')
    },
    onError: () => {
      showToast({
        title: 'No se pudo enviar el enlace',
        description: 'Revisa la sucursal y el horario propuesto.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await rescheduleMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button
            onClick={() => navigate(appointmentId ? `/appointments/${appointmentId}` : '/appointments')}
            variant="secondary"
          >
            Volver al detalle
          </Button>
        }
        description="Cambia la fecha, duración o ubicación de la cita y deja el historial en estado reprogramado."
        eyebrow="Reprogramacion"
        title="Reprogramar cita"
      />

      {bookingQuery.isPending ? (
        <LoadingState message="Cargando la cita para reprogramarla." variant="detail" />
      ) : bookingQuery.isError || !bookingQuery.data ? (
        <ErrorState
          description="No pudimos cargar la cita seleccionada."
          onRetry={() => void bookingQuery.refetch()}
          title="No fue posible abrir la cita"
        />
      ) : (
        <Card>
          <form className="space-y-5" onSubmit={onSubmit}>
            <div className="grid gap-5 md:grid-cols-2">
              <Input
                error={errors.startsAt?.message}
                label="Nueva fecha y hora"
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

            <Textarea
              error={errors.notes?.message}
              label="Notas de reprogramacion"
              rows={6}
              {...register('notes')}
            />

            <div className="flex flex-wrap justify-end gap-3">
              <Button
                onClick={() => navigate(`/appointments/${bookingQuery.data.id}`)}
                variant="secondary"
              >
                Cancelar
              </Button>
              <Button
                disabled={!isOnline}
                loading={rescheduleLinkMutation.isPending}
                onClick={handleSubmit((values) => rescheduleLinkMutation.mutate(values))}
                type="button"
                variant="secondary"
              >
                Enviar enlace publico
              </Button>
              <Button
                disabled={!isOnline}
                loading={rescheduleMutation.isPending || isSubmitting}
                type="submit"
              >
                Guardar nueva fecha
              </Button>
            </div>
          </form>
        </Card>
      )}
    </section>
  )
}
