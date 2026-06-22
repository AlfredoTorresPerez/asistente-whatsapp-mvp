import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { createResponseTemplateRequest } from '../../../services/api/conversationsApi'

const schema = z.object({
  name: z.string().trim().min(1, 'Ingresa un nombre.').max(120),
  category: z.string().trim().min(1, 'Ingresa una categoria.').max(50),
  body: z.string().trim().min(1, 'Ingresa el contenido de la plantilla.').max(4000),
  active: z.boolean(),
})

type FormValues = z.infer<typeof schema>

export function NewTemplatePage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      category: 'FOLLOW_UP',
      body: '',
      active: true,
    },
  })

  const createMutation = useMutation({
    mutationFn: createResponseTemplateRequest,
    onSuccess: () => {
      showToast({
        title: 'Plantilla creada',
        description: 'La nueva respuesta ya esta disponible en el detalle de conversaciones.',
        tone: 'success',
      })
      navigate('/templates')
    },
    onError: () => {
      showToast({
        title: 'No se pudo crear la plantilla',
        description: 'Revisa el formulario e intenta nuevamente.',
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
          <Button onClick={() => navigate('/templates')} variant="secondary">
            Volver a plantillas
          </Button>
        }
        description="Alta manual de una plantilla reusable con categoria, estado y variables simples."
        eyebrow="Nueva plantilla"
        title="Crear plantilla"
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <Card>
          <form className="space-y-5" onSubmit={onSubmit}>
            <Input error={errors.name?.message} label="Nombre" {...register('name')} />

            <Input error={errors.category?.message} label="Categoria" {...register('category')} />

            <Textarea
              error={errors.body?.message}
              hint="Variables disponibles: {{customer_name}}, {{customer_phone}}, {{business_name}}, {{agent_name}}."
              label="Contenido"
              rows={8}
              {...register('body')}
            />

            <label className="flex items-center gap-3 rounded-[18px] border border-[var(--color-border)] bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700">
              <input className="h-4 w-4" type="checkbox" {...register('active')} />
              Crear como plantilla activa
            </label>

            <div className="flex flex-wrap justify-end gap-3">
              <Button onClick={() => navigate('/templates')} variant="secondary">
                Cancelar
              </Button>
              <Button
                disabled={!isOnline}
                loading={createMutation.isPending || isSubmitting}
                type="submit"
              >
                Guardar plantilla
              </Button>
            </div>
          </form>
        </Card>

        <Card className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
            Recomendaciones
          </p>
          <ul className="space-y-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            <li>Usa nombres faciles de encontrar dentro del detalle de conversaciones.</li>
            <li>Las variables simples se reemplazan automaticamente al enviar.</li>
            <li>Si la plantilla no debe aparecer en el selector, crea el registro desactivado.</li>
          </ul>
        </Card>
      </div>
    </section>
  )
}
