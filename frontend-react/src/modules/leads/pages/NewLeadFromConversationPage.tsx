import { zodResolver } from '@hookform/resolvers/zod'
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
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { getConversationDetailRequest } from '../../../services/api/conversationsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import { createLeadFromConversationRequest } from '../../../services/api/leadsApi'
import { leadStageOptions } from '../leadOptions'

const schema = z.object({
  firstName: z.string().trim().min(1, 'Ingresa el nombre.').max(80),
  lastName: z.string().trim().min(1, 'Ingresa el apellido.').max(80),
  phone: z.string().trim().min(8, 'Ingresa un telefono valido.').max(30),
  email: z.string().trim().max(255).email('Ingresa un correo valido.').or(z.literal('')),
  stage: z.string().trim().min(1, 'Selecciona una etapa.'),
  notes: z.string().trim().max(2000, 'La nota no puede superar los 2000 caracteres.'),
})

type FormValues = z.infer<typeof schema>

function splitName(displayName: string) {
  const parts = displayName.trim().split(/\s+/, 2)
  if (parts.length === 1) {
    return { firstName: parts[0], lastName: parts[0] }
  }
  return { firstName: parts[0], lastName: parts[1] }
}

export function NewLeadFromConversationPage() {
  const { conversationId } = useParams()
  const navigate = useNavigate()
  const { user } = useShellSession()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      stage: 'NEW',
      notes: '',
    },
  })

  const conversationQuery = useQuery({
    queryKey: ['conversations', 'detail', conversationId],
    queryFn: () => getConversationDetailRequest(conversationId ?? ''),
    enabled: Boolean(conversationId),
  })

  useEffect(() => {
    if (!conversationQuery.data) {
      return
    }

    const nameParts = splitName(conversationQuery.data.customer.displayName)
    reset({
      firstName: conversationQuery.data.customer.firstName || nameParts.firstName,
      lastName: conversationQuery.data.customer.lastName || nameParts.lastName,
      phone: conversationQuery.data.customer.phone,
      email: conversationQuery.data.customer.email ?? '',
      stage: 'NEW',
      notes: conversationQuery.data.lastMessagePreview ?? '',
    })
  }, [conversationQuery.data, reset])

  const createMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!conversationId) {
        throw new Error('No hay conversacion seleccionada.')
      }

      return createLeadFromConversationRequest(conversationId, {
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone,
        email: values.email || undefined,
        stage: values.stage,
        notes: values.notes || undefined,
        assignedUserId: user?.id,
      })
    },
    onSuccess: (lead) => {
      showToast({
        title: 'Prospecto creado desde la conversacion',
        description: 'El contacto ya quedo vinculado al embudo comercial.',
        tone: 'success',
      })
      navigate(`/prospects/${lead.id}`)
    },
    onError: (error) => {
      showToast({
        title: 'No se pudo crear el prospecto',
        description: error instanceof ApiClientError
          ? error.message
          : 'La conversacion podria tener ya un prospecto asociado o faltar algun dato.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    try {
      await createMutation.mutateAsync(values)
    } catch {
      // El toast de onError informa la causa y evita promesas no capturadas en consola.
    }
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button
            onClick={() => navigate(conversationId ? `/conversations/${conversationId}` : '/conversations')}
            variant="secondary"
          >
            Volver a la conversacion
          </Button>
        }
        description="Convierte la conversacion activa en un prospecto comercial reutilizando el contexto del contacto."
        eyebrow="Desde conversacion"
        title="Crear prospecto"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Debes recuperar internet para asociar el prospecto a la conversacion.
          </p>
        </Card>
      ) : null}

      {conversationQuery.isPending ? (
        <LoadingState
          message="Cargando los datos de la conversacion para prellenar el prospecto."
          variant="detail"
        />
      ) : conversationQuery.isError || !conversationQuery.data ? (
        <ErrorState
          description="No pudimos recuperar la conversacion para crear el prospecto."
          onRetry={() => void conversationQuery.refetch()}
          title="No fue posible abrir la conversacion"
        />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
          <Card>
            <form className="space-y-5" onSubmit={onSubmit}>
              <div className="grid gap-5 md:grid-cols-2">
                <Input
                  error={errors.firstName?.message}
                  label="Nombre"
                  {...register('firstName')}
                />
                <Input
                  error={errors.lastName?.message}
                  label="Apellido"
                  {...register('lastName')}
                />
              </div>

              <div className="grid gap-5 md:grid-cols-2">
                <Input
                  error={errors.phone?.message}
                  label="Telefono"
                  {...register('phone')}
                />
                <Input
                  error={errors.email?.message}
                  label="Correo"
                  type="email"
                  {...register('email')}
                />
              </div>

              <Select
                error={errors.stage?.message}
                label="Etapa"
                options={leadStageOptions.map((option) => ({
                  label: option.label,
                  value: option.value,
                }))}
                {...register('stage')}
              />

              <Textarea
                error={errors.notes?.message}
                hint="Puedes editar el contexto heredado desde la conversacion."
                label="Nota inicial"
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
                <Button disabled={!isOnline} loading={createMutation.isPending || isSubmitting} type="submit">
                  Crear prospecto
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
              <li>Estado de la conversacion: {conversationQuery.data.status}</li>
              <li>
                Ultimo mensaje: {conversationQuery.data.lastMessagePreview ?? 'Sin mensaje reciente'}
              </li>
            </ul>
          </Card>
        </div>
      )}
    </section>
  )
}
