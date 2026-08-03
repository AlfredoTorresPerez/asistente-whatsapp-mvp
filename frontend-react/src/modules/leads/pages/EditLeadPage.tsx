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
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { getLeadDetailRequest, updateLeadRequest } from '../../../services/api/leadsApi'
import { leadStageOptions } from '../leadOptions'

const schema = z.object({
  firstName: z.string().trim().min(1, 'Ingresa el nombre.').max(80),
  lastName: z.string().trim().min(1, 'Ingresa el apellido.').max(80),
  phone: z.string().trim().min(8, 'Ingresa un telefono valido.').max(30),
  email: z.string().trim().max(255).email('Ingresa un correo valido.').or(z.literal('')),
  stage: z.string().trim().min(1, 'Selecciona una etapa.'),
  locationId: z.string(),
  notes: z.string().trim().max(2000, 'La nota no puede superar los 2000 caracteres.'),
})

type FormValues = z.infer<typeof schema>

export function EditLeadPage() {
  const { prospectId } = useParams()
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
      locationId: '',
      notes: '',
    },
  })

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'active'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const leadQuery = useQuery({
    queryKey: ['leads', 'detail', prospectId],
    queryFn: () => getLeadDetailRequest(prospectId ?? ''),
    enabled: Boolean(prospectId),
  })

  useEffect(() => {
    if (!leadQuery.data) {
      return
    }

    reset({
      firstName: leadQuery.data.firstName,
      lastName: leadQuery.data.lastName,
      phone: leadQuery.data.phone,
      email: leadQuery.data.email ?? '',
      stage: leadQuery.data.stage,
      locationId: leadQuery.data.locationId ?? '',
      notes: leadQuery.data.notes ?? '',
    })
  }, [leadQuery.data, reset])

  const updateMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!prospectId) {
        throw new Error('No hay prospecto seleccionado.')
      }

      return updateLeadRequest(prospectId, {
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone,
        email: values.email || undefined,
        stage: values.stage,
        notes: values.notes || undefined,
        assignedUserId: user?.id,
        locationId: values.locationId || undefined,
      })
    },
    onSuccess: (lead) => {
      showToast({
        title: 'Prospecto actualizado',
        description: 'La ficha comercial ya refleja los cambios.',
        tone: 'success',
      })
      navigate(`/prospects/${lead.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo actualizar el prospecto',
        description: 'Revisa los datos y vuelve a intentarlo.',
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
            onClick={() => navigate(prospectId ? `/prospects/${prospectId}` : '/prospects')}
            variant="secondary"
          >
            Volver al detalle
          </Button>
        }
        description="Actualiza datos de contacto, etapa y nota principal del prospecto seleccionado."
        eyebrow="Editar prospecto"
        title="Editar prospecto"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Debes recuperar internet para guardar cambios en el prospecto.
          </p>
        </Card>
      ) : null}

      {leadQuery.isPending ? (
        <LoadingState
          message="Preparando la ficha del prospecto para editar sus datos."
          variant="detail"
        />
      ) : leadQuery.isError || !leadQuery.data ? (
        <ErrorState
          description="No pudimos cargar la informacion del prospecto para editarla."
          onRetry={() => void leadQuery.refetch()}
          title="No fue posible abrir el prospecto"
        />
      ) : (
        <Card>
          <form className="space-y-5" onSubmit={onSubmit}>
            <div className="grid gap-5 md:grid-cols-2">
              <Input error={errors.firstName?.message} label="Nombre" {...register('firstName')} />
              <Input error={errors.lastName?.message} label="Apellido" {...register('lastName')} />
            </div>

            <div className="grid gap-5 md:grid-cols-2">
              <Input error={errors.phone?.message} label="Telefono" {...register('phone')} />
              <Input
                error={errors.email?.message}
                label="Correo"
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
              label="Nota principal"
              rows={6}
              {...register('notes')}
            />

            <div className="flex flex-wrap justify-end gap-3">
              <Button
                onClick={() => navigate(`/prospects/${leadQuery.data.id}`)}
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
