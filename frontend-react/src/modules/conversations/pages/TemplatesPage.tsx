import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Modal } from '../../../components/overlay/Modal'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  getResponseTemplatesRequest,
  updateResponseTemplateRequest,
  updateTemplateStatusRequest,
} from '../../../services/api/conversationsApi'
import type { ResponseTemplateResponse } from '../../../services/api/types'

const schema = z.object({
  name: z.string().trim().min(1, 'Ingresa un nombre.').max(120),
  category: z.string().trim().min(1, 'Ingresa una categoria.').max(50),
  body: z.string().trim().min(1, 'Ingresa el contenido de la plantilla.').max(4000),
})

type FormValues = z.infer<typeof schema>

export function TemplatesPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const [editingTemplateId, setEditingTemplateId] = useState<string | null>(null)
  const templatesQuery = useQuery({
    queryKey: ['templates', 'list'],
    queryFn: () => getResponseTemplatesRequest(),
    refetchInterval: isOnline ? 30_000 : false,
  })

  const editingTemplate = templatesQuery.data?.find((template) => template.id === editingTemplateId)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      category: '',
      body: '',
    },
  })

  useEffect(() => {
    if (!editingTemplate) {
      return
    }

    form.reset({
      name: editingTemplate.name,
      category: editingTemplate.category,
      body: editingTemplate.body,
    })
  }, [editingTemplate, form])

  const updateMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!editingTemplate) {
        throw new Error('No hay plantilla seleccionada.')
      }
      return updateResponseTemplateRequest(editingTemplate.id, values)
    },
    onSuccess: async () => {
      setEditingTemplateId(null)
      await queryClient.invalidateQueries({ queryKey: ['templates'] })
      showToast({
        title: 'Plantilla actualizada',
        description: 'Los cambios quedaron guardados correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo actualizar la plantilla',
        description: 'Revisa el formulario y vuelve a intentarlo.',
        tone: 'error',
      })
    },
  })

  const statusMutation = useMutation({
    mutationFn: async (template: ResponseTemplateResponse) =>
      updateTemplateStatusRequest(template.id, { active: !template.active }),
    onSuccess: async (template) => {
      await queryClient.invalidateQueries({ queryKey: ['templates'] })
      showToast({
        title: template.active ? 'Plantilla activada' : 'Plantilla desactivada',
        description: 'El estado de la plantilla se actualizo correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo cambiar el estado',
        description: 'Intenta nuevamente en unos segundos.',
        tone: 'error',
      })
    },
  })

  const closeEditModal = () => {
    setEditingTemplateId(null)
  }

  const onSubmit = form.handleSubmit(async (values) => {
    await updateMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button onClick={() => navigate('/templates/new')}>Crear plantilla</Button>
            <Button onClick={() => navigate('/conversations')} variant="secondary">
              Volver a conversaciones
            </Button>
          </>
        }
        description="Repositorio de respuestas reutilizables para aplicar dentro del detalle de conversaciones."
        eyebrow="Plantillas"
        title="Plantillas de respuesta"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Puedes revisar las plantillas ya cargadas, pero no se podran guardar cambios hasta
            recuperar internet.
          </p>
        </Card>
      ) : null}

      {templatesQuery.isPending && !templatesQuery.data ? (
        <LoadingState
          message="Cargando las plantillas activas e inactivas del entorno demo."
          variant="table"
        />
      ) : null}

      {templatesQuery.isError && !templatesQuery.data ? (
        <ErrorState
          description="No pudimos recuperar las plantillas. Reintenta para volver a cargar el listado."
          onRetry={() => void templatesQuery.refetch()}
          title="No fue posible cargar las plantillas"
        />
      ) : null}

      {templatesQuery.data && templatesQuery.data.length === 0 ? (
        <EmptyState
          description="Todavia no hay plantillas registradas en este negocio demo."
          primaryAction={{ label: 'Crear plantilla', to: '/templates/new' }}
          secondaryAction={{ label: 'Volver a conversaciones', to: '/conversations' }}
          title="Sin plantillas"
        />
      ) : null}

      {templatesQuery.data && templatesQuery.data.length > 0 ? (
        <Card className="p-0">
          <div className="overflow-x-auto">
            <table className="min-w-full border-separate border-spacing-0">
              <thead>
                <tr className="bg-slate-50">
                  {['Plantilla', 'Categoria', 'Estado', 'Actualizada', 'Acciones'].map((column) => (
                    <th
                      key={column}
                      className="border-b border-[var(--color-border)] px-5 py-3 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-500"
                      scope="col"
                    >
                      {column}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white">
                {templatesQuery.data.map((template) => (
                  <tr key={template.id} className="align-top transition hover:bg-slate-50">
                    <td className="border-b border-[var(--color-border)] px-5 py-4">
                      <p className="text-sm font-semibold text-slate-950">{template.name}</p>
                      <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
                        {template.body}
                      </p>
                    </td>
                    <td className="border-b border-[var(--color-border)] px-5 py-4">
                      <StatusBadge label={template.category} tone="info" />
                    </td>
                    <td className="border-b border-[var(--color-border)] px-5 py-4">
                      <StatusBadge
                        label={template.active ? 'Activa' : 'Inactiva'}
                        tone={template.active ? 'success' : 'neutral'}
                      />
                    </td>
                    <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-600">
                      {dayjs(template.updatedAt).format('DD MMM YYYY HH:mm')}
                    </td>
                    <td className="border-b border-[var(--color-border)] px-5 py-4">
                      <div className="flex flex-wrap justify-end gap-2">
                        <Button
                          onClick={() => setEditingTemplateId(template.id)}
                          size="sm"
                          variant="secondary"
                        >
                          Editar
                        </Button>
                        <Button
                          disabled={!isOnline}
                          loading={
                            statusMutation.isPending && statusMutation.variables?.id === template.id
                          }
                          onClick={() => void statusMutation.mutateAsync(template)}
                          size="sm"
                          variant={template.active ? 'ghost' : 'primary'}
                        >
                          {template.active ? 'Desactivar' : 'Activar'}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}

      <Modal
        maxWidthClassName="max-w-[720px]"
        onClose={closeEditModal}
        open={Boolean(editingTemplate)}
      >
        <form className="space-y-5" onSubmit={onSubmit}>
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
              Editar plantilla
            </p>
            <h2 className="mt-3 text-[28px] font-semibold text-[var(--color-text)]">
              Ajustar contenido reutilizable
            </h2>
          </div>

          <Input
            error={form.formState.errors.name?.message}
            label="Nombre"
            {...form.register('name')}
          />

          <Input
            error={form.formState.errors.category?.message}
            label="Categoria"
            {...form.register('category')}
          />

          <Textarea
            error={form.formState.errors.body?.message}
            hint="Variables disponibles: {{customer_name}}, {{customer_phone}}, {{business_name}}, {{agent_name}}."
            label="Contenido"
            rows={8}
            {...form.register('body')}
          />

          <div className="flex flex-wrap justify-end gap-3">
            <Button onClick={closeEditModal} variant="secondary">
              Cancelar
            </Button>
            <Button disabled={!isOnline} loading={updateMutation.isPending} type="submit">
              Guardar cambios
            </Button>
          </div>
        </form>
      </Modal>
    </section>
  )
}
