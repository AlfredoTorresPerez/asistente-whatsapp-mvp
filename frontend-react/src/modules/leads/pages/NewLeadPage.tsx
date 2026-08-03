import { zodResolver } from '@hookform/resolvers/zod'
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
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { createLeadRequest } from '../../../services/api/leadsApi'
import { leadStageOptions } from '../leadOptions'

const schema = z.object({
  firstName: z.string().trim().min(1, 'Ingresa el nombre.').max(80),
  lastName: z.string().trim().min(1, 'Ingresa el apellido.').max(80),
  phone: z.string().trim().min(8, 'Ingresa un telefono valido.').max(30),
  email: z.string().trim().max(255).email('Ingresa un correo valido.').or(z.literal('')),
  stage: z.string().trim().min(1, 'Selecciona una etapa.'),
  locationId: z.string(),
  notes: z.string().trim().max(2000, 'La nota inicial no puede superar los 2000 caracteres.'),
})

type FormValues = z.infer<typeof schema>

export function NewLeadPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const { user } = useShellSession()
  const isOnline = useOnlineStatus()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      stage: 'NEW',
      locationId: '',
      notes: '',
    },
  })

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const createMutation = useMutation({
    mutationFn: createLeadRequest,
    onSuccess: (lead) => {
      showToast({
        title: 'Prospecto creado',
        description: 'El prospecto ya forma parte del embudo comercial.',
        tone: 'success',
      })
      navigate(`/prospects/${lead.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo crear el prospecto',
        description: 'Revisa los datos del formulario y vuelve a intentarlo.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync({
      firstName: values.firstName,
      lastName: values.lastName,
      phone: values.phone,
      email: values.email || undefined,
      stage: values.stage,
      notes: values.notes || undefined,
      assignedUserId: user?.id,
      locationId: values.locationId || undefined,
    })
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button onClick={() => navigate('/prospects')} variant="secondary">
            Volver a prospectos
          </Button>
        }
        description="Alta manual de prospectos para iniciar seguimiento comercial sin depender de una conversación previa."
        eyebrow="Nuevo prospecto"
        title="Crear prospecto"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Debes recuperar internet para guardar un prospecto nuevo.
          </p>
        </Card>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <Card>
          <form className="space-y-5" onSubmit={onSubmit}>
            <div className="grid gap-5 md:grid-cols-2">
              <Input
                error={errors.firstName?.message}
                label="Nombre"
                placeholder="Ej. Sofia"
                {...register('firstName')}
              />
              <Input
                error={errors.lastName?.message}
                label="Apellido"
                placeholder="Ej. Rojas"
                {...register('lastName')}
              />
            </div>

            <div className="grid gap-5 md:grid-cols-2">
              <Input
                error={errors.phone?.message}
                label="Telefono"
                placeholder="Ej. +56 9 XXXX XXXX"
                {...register('phone')}
              />
              <Input
                error={errors.email?.message}
                label="Correo"
                placeholder="cliente@dominio.cl"
                type="email"
                {...register('email')}
              />
            </div>

            <div className="grid gap-5 md:grid-cols-2">
              <Select
                error={errors.stage?.message}
                label="Etapa"
                options={leadStageOptions.map((option) => ({
                  label: option.label,
                  value: option.value,
                }))}
                {...register('stage')}
              />
              <Select
                label="Sucursal"
                options={[
                  { label: 'Sin sucursal asignada', value: '' },
                  ...(locationsQuery.data ?? []).map((location) => ({
                    label: location.name,
                    value: location.id,
                  })),
                ]}
                {...register('locationId')}
              />
            </div>

            <Textarea
              error={errors.notes?.message}
              hint="Opcional. Puedes dejar contexto comercial o motivo del alta."
              label="Nota inicial"
              placeholder="Ej. Llegó por recomendacion y quiere una evaluacion esta semana."
              rows={6}
              {...register('notes')}
            />

            <div className="flex flex-wrap justify-end gap-3">
              <Button onClick={() => navigate('/prospects')} variant="secondary">
                Cancelar
              </Button>
              <Button
                disabled={!isOnline}
                loading={createMutation.isPending || isSubmitting}
                type="submit"
              >
                Crear prospecto
              </Button>
            </div>
          </form>
        </Card>

        <Card className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
            Resumen
          </p>
          <h2 className="text-2xl font-semibold text-[var(--color-text)]">Checklist rapido</h2>
          <ul className="space-y-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            <li>Si el telefono ya existe, se asociara el cliente existente.</li>
            <li>La etapa inicial puede ajustarse al estado comercial real.</li>
            <li>El prospecto quedara asignado al usuario autenticado.</li>
          </ul>
        </Card>
      </div>
    </section>
  )
}
