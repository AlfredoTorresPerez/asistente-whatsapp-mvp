import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { sendWhatsAppSimulationRequest } from '../../../services/api/administrationApi'

const simSchema = z.object({
  customerName: z.string().trim().min(1, 'Ingresa el nombre del cliente.'),
  from: z
    .string()
    .trim()
    .min(1, 'Ingresa el numero de telefono.')
    .regex(/^\+?\d{7,15}$/, 'Formato invalido. Ej: 56912345678'),
  body: z
    .string()
    .trim()
    .min(1, 'Ingresa un mensaje.')
    .max(2000, 'El mensaje no puede superar los 2000 caracteres.'),
})

type SimValues = z.infer<typeof simSchema>

export function WhatsAppSimulatorPage() {
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SimValues>({
    resolver: zodResolver(simSchema),
    defaultValues: {
      customerName: '',
      from: '56912345678',
      body: '',
    },
  })

  const simMutation = useMutation({
    mutationFn: (values: SimValues) =>
      sendWhatsAppSimulationRequest({
        from: values.from,
        body: values.body,
        externalMessageId: `sim-${Date.now()}`,
        sessionKey: 'demo-sales',
      }),
    onSuccess: () => {
      reset()
      showToast({
        title: 'Simulacion enviada',
        description: 'El mensaje fue procesado correctamente. Revisa la bandeja de conversaciones.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'Error al simular',
        description: 'No pudimos procesar la simulacion. Verifica que el backend este disponible.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit((values) => {
    void simMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        description="Simula mensajes entrantes de WhatsApp sin usar Postman. Los mensajes se procesan como si llegaran desde un cliente real."
        eyebrow="Administracion"
        title="Simulador WhatsApp"
      />

      <Card className="space-y-5">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
            Mensaje de simulacion
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-950">
            Simular mensaje entrante
          </h2>
        </div>

        <form className="grid gap-4 md:grid-cols-2" onSubmit={onSubmit}>
          <Input
            error={errors.customerName?.message}
            label="Nombre del cliente"
            placeholder="Ej: Maria Garcia"
            {...register('customerName')}
          />
          <Input
            error={errors.from?.message}
            hint="Formato: 56912345678"
            label="Numero de telefono"
            placeholder="56912345678"
            {...register('from')}
          />
          <div className="md:col-span-2">
            <Textarea
              error={errors.body?.message}
              label="Mensaje de texto"
              placeholder="Ej: Hola, quiero agendar una hora para un corte de cabello."
              {...register('body')}
            />
          </div>

          <div className="md:col-span-2 flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-5">
            <Link to="/conversations">
              <Button variant="secondary">Ir a conversaciones</Button>
            </Link>
            <Button
              disabled={simMutation.isPending}
              loading={simMutation.isPending}
              type="submit"
            >
              Enviar simulacion
            </Button>
          </div>
        </form>
      </Card>

      {simMutation.isSuccess ? (
        <Card className="border-emerald-200 bg-emerald-50">
          <p className="text-sm font-semibold text-emerald-900">Simulacion exitosa</p>
          <p className="mt-1 text-sm text-emerald-700">
            El mensaje se envio correctamente.{' '}
            <Link className="underline" to="/conversations">
              Ver conversaciones
            </Link>
          </p>
        </Card>
      ) : null}

      {simMutation.isError ? (
        <Card className="border-red-200 bg-red-50">
          <p className="text-sm font-semibold text-red-900">Error en la simulacion</p>
          <p className="mt-1 text-sm text-red-700">
            No pudimos procesar el mensaje. Revisa que el backend este disponible.
          </p>
        </Card>
      ) : null}
    </section>
  )
}
