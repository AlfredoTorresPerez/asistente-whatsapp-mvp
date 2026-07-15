import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Textarea } from '../../../components/ui/Textarea'
import { Button } from '../../../components/ui/Button'
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { createConversationRequest } from '../../../services/api/conversationsApi'

const schema = z.object({
  customerName: z.string().trim().min(1, 'Ingresa el nombre del cliente.').max(160),
  customerPhone: z.string().trim().min(8, 'Ingresa un telefono valido.').max(30),
  customerEmail: z.string().trim().max(255).email('Ingresa un correo valido.').or(z.literal('')),
  initialMessage: z.string().trim().max(1000, 'El mensaje no puede superar los 1000 caracteres.'),
})

type FormValues = z.infer<typeof schema>

export function NewConversationPage() {
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
      customerName: '',
      customerPhone: '',
      customerEmail: '',
      initialMessage: '',
    },
  })

  const createMutation = useMutation({
    mutationFn: createConversationRequest,
    onSuccess: (conversation) => {
      showToast({
        title: 'Conversacion creada',
        description: 'La nueva conversacion ya esta lista para continuar la atencion.',
        tone: 'success',
      })
      navigate(`/conversations/${conversation.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo crear la conversacion',
        description: 'Revisa los datos del cliente y vuelve a intentarlo.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync({
      customerName: values.customerName,
      customerPhone: values.customerPhone,
      customerEmail: values.customerEmail || undefined,
      ownerUserId: user?.id,
      initialMessage: values.initialMessage || undefined,
    })
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button onClick={() => navigate('/conversations')} variant="secondary">
            Volver a conversaciones
          </Button>
        }
        description="Formulario manual para iniciar una nueva conversacion y, si hace falta, dejar listo el primer mensaje del hilo."
        eyebrow="Nueva conversacion"
        title="Crear conversacion"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Debes recuperar internet para crear una conversacion nueva y enviar mensajes por el
            canal.
          </p>
        </Card>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <Card>
          <form className="space-y-5" onSubmit={onSubmit}>
            <Input
              error={errors.customerName?.message}
              label="Nombre del cliente"
              placeholder="Ej. Sofia Rojas"
              {...register('customerName')}
            />

            <Input
              error={errors.customerPhone?.message}
              label="Telefono"
              placeholder="+56911112222"
              {...register('customerPhone')}
            />

            <Input
              error={errors.customerEmail?.message}
              label="Correo electronico"
              placeholder="cliente@demo.cl"
              type="email"
              {...register('customerEmail')}
            />

            <Textarea
              error={errors.initialMessage?.message}
              hint="Opcional. Si lo completas, se enviara al crear la conversacion."
              label="Primer mensaje"
              placeholder="Hola, te escribo para continuar con tu atencion..."
              rows={6}
              {...register('initialMessage')}
            />

            <div className="flex flex-wrap justify-end gap-3">
              <Button onClick={() => navigate('/conversations')} variant="secondary">
                Cancelar
              </Button>
              <Button
                disabled={!isOnline}
                loading={createMutation.isPending || isSubmitting}
                type="submit"
              >
                Crear conversacion
              </Button>
            </div>
          </form>
        </Card>

        <Card className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
            Snapshot
          </p>
          <h2 className="text-2xl font-semibold text-[var(--color-text)]">Checklist rapido</h2>
          <ul className="space-y-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            <li>Se asociara el cliente por telefono si ya existe en la base demo.</li>
            <li>La conversacion nacera asignada al usuario actual.</li>
            <li>El mensaje inicial es opcional, pero sirve para validar el envio real.</li>
          </ul>
        </Card>
      </div>
    </section>
  )
}
