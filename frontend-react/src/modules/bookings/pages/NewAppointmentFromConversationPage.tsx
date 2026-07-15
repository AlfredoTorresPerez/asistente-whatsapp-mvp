import { zodResolver } from '@hookform/resolvers/zod'
import dayjs from 'dayjs'
import { useMutation, useQuery } from '@tanstack/react-query'
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
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { createBookingFromConversationRequest } from '../../../services/api/bookingsApi'
import { getConversationDetailRequest } from '../../../services/api/conversationsApi'
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

export function NewAppointmentFromConversationPage() {
  const { conversationId } = useParams()
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
      subject: 'Seguimiento comercial',
      startsAt: dayjs().add(1, 'day').hour(10).minute(0).format('YYYY-MM-DDTHH:mm'),
      durationMinutes: 60,
      status: 'PENDIENTE_CONFIRMACION',
      locationId: '',
      location: '',
      notes: '',
    },
  })

  const conversationQuery = useQuery({
    queryKey: ['conversations', 'detail', conversationId],
    queryFn: () => getConversationDetailRequest(conversationId ?? ''),
    enabled: Boolean(conversationId),
  })

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const createMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!conversationId) {
        throw new Error('No hay conversación seleccionada.')
      }

      return createBookingFromConversationRequest(conversationId, {
        subject: values.subject,
        startsAt: dayjs(values.startsAt).toISOString(),
        durationMinutes: values.durationMinutes,
        status: values.status,
        locationId: values.locationId || undefined,
        location: values.location || undefined,
        notes: values.notes || undefined,
        assignedUserId: user?.id,
      })
    },
    onSuccess: (booking) => {
      showToast({
        title: 'Cita creada desde la conversación',
        description: 'La agenda ya quedó vinculada al hilo comercial.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo crear la cita',
        description: 'Revisa la información de la conversación y vuelve a intentarlo.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button
            onClick={() =>
              navigate(conversationId ? `/conversations/${conversationId}` : '/conversations')
            }
            variant="secondary"
          >
            Volver a la conversación
          </Button>
        }
        description="Agenda una cita reutilizando el contexto del contacto y el hilo comercial activo."
        eyebrow="Desde conversación"
        title="Crear cita"
      />

      {conversationQuery.isPending ? (
        <LoadingState
          message="Cargando la conversación para contextualizar la cita."
          variant="detail"
        />
      ) : conversationQuery.isError || !conversationQuery.data ? (
        <ErrorState
          description="No pudimos recuperar la conversación seleccionada."
          onRetry={() => void conversationQuery.refetch()}
          title="No fue posible abrir la conversación"
        />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
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

              <Textarea
                error={errors.notes?.message}
                label="Notas"
                rows={6}
                {...register('notes')}
              />

              <div className="flex flex-wrap justify-end gap-3">
                <Button
                  onClick={() => navigate(`/conversations/${conversationQuery.data.id}`)}
                  variant="secondary"
                >
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

          <Card className="space-y-4">
            <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
              Contexto del hilo
            </p>
            <h2 className="text-2xl font-semibold text-[var(--color-text)]">
              {conversationQuery.data.customer.displayName}
            </h2>
            <ul className="space-y-3 text-sm leading-6 text-[var(--color-text-secondary)]">
              <li>Telefono: {conversationQuery.data.customer.phone}</li>
              <li>Estado del hilo: {conversationQuery.data.status}</li>
              <li>
                Último mensaje:{' '}
                {conversationQuery.data.lastMessagePreview ?? 'Sin mensaje reciente'}
              </li>
            </ul>
          </Card>
        </div>
      )}
    </section>
  )
}
