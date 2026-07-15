import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  getCompanySettingsRequest,
  updateCompanySettingsRequest,
} from '../../../services/api/administrationApi'

const companySettingsSchema = z.object({
  companyName: z.string().trim().min(1, 'Ingresa la razon social.').max(150),
  businessName: z.string().trim().min(1, 'Ingresa el nombre comercial.').max(150),
  timezone: z.string().trim().min(1, 'Selecciona una zona horaria.'),
  currency: z.string().trim().length(3, 'La moneda debe tener 3 caracteres.'),
  contactEmail: z.email('Ingresa un correo valido.'),
  supportPhone: z
    .string()
    .trim()
    .refine((value) => value === '' || /^\+?[1-9]\d{7,14}$/.test(value), {
      message: 'Ingresa un telefono valido en formato internacional.',
    }),
  address: z.string().trim().max(255, 'La direccion no puede superar los 255 caracteres.'),
})

type CompanySettingsValues = z.infer<typeof companySettingsSchema>

const timezoneOptions = [
  { label: 'America/Santiago', value: 'America/Santiago' },
  { label: 'America/Lima', value: 'America/Lima' },
  { label: 'America/Bogota', value: 'America/Bogota' },
]

const currencyOptions = [
  { label: 'CLP', value: 'CLP' },
  { label: 'USD', value: 'USD' },
  { label: 'EUR', value: 'EUR' },
]

export function CompanySettingsPage() {
  const navigate = useNavigate()
  const isOnline = useOnlineStatus()
  const { showToast } = useToast()
  const { syncBusinessName } = useShellSession()
  const companyQuery = useQuery({
    queryKey: ['administration', 'company'],
    queryFn: getCompanySettingsRequest,
  })
  const {
    handleSubmit,
    register,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<CompanySettingsValues>({
    resolver: zodResolver(companySettingsSchema),
    defaultValues: {
      companyName: '',
      businessName: '',
      timezone: 'America/Santiago',
      currency: 'CLP',
      contactEmail: '',
      supportPhone: '',
      address: '',
    },
  })

  useEffect(() => {
    if (!companyQuery.data) {
      return
    }

    reset({
      companyName: companyQuery.data.companyName,
      businessName: companyQuery.data.businessName,
      timezone: companyQuery.data.timezone,
      currency: companyQuery.data.currency,
      contactEmail: companyQuery.data.contactEmail,
      supportPhone: companyQuery.data.supportPhone ?? '',
      address: companyQuery.data.address ?? '',
    })
  }, [companyQuery.data, reset])

  const updateCompanyMutation = useMutation({
    mutationFn: updateCompanySettingsRequest,
    onSuccess: (response) => {
      syncBusinessName(response.businessName)
      reset({
        companyName: response.companyName,
        businessName: response.businessName,
        timezone: response.timezone,
        currency: response.currency,
        contactEmail: response.contactEmail,
        supportPhone: response.supportPhone ?? '',
        address: response.address ?? '',
      })
      showToast({
        title: 'Configuracion guardada',
        description: 'La informacion principal del negocio se actualizo correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos guardar la empresa',
        description: 'Revisa los campos y vuelve a intentarlo en unos segundos.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await updateCompanyMutation.mutateAsync(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button
              disabled={!isOnline || updateCompanyMutation.isPending || !isDirty}
              loading={updateCompanyMutation.isPending || isSubmitting}
              onClick={() => void onSubmit()}
            >
              Guardar cambios
            </Button>
            <Button onClick={() => navigate('/admin')} variant="secondary">
              Cancelar
            </Button>
          </>
        }
        description="Actualiza la razon social, el nombre comercial y los datos operativos base que usa el shell privado."
        eyebrow="Administracion"
        title="Configuracion de empresa"
      />

      {companyQuery.isPending && !companyQuery.data ? (
        <LoadingState message="Cargando la configuracion actual de la empresa." variant="page" />
      ) : null}

      {companyQuery.isError && !companyQuery.data ? (
        <ErrorState
          description="No pudimos recuperar la configuracion del negocio. Reintenta para volver a editarla."
          onRetry={() => void companyQuery.refetch()}
          title="No fue posible cargar la empresa"
        />
      ) : null}

      {companyQuery.data ? (
        <form className="space-y-4" onSubmit={onSubmit}>
          <div className="grid gap-4 xl:grid-cols-[1.2fr_0.9fr]">
            <Card className="space-y-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                    Datos corporativos
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold text-slate-950">
                    Identidad del negocio
                  </h2>
                </div>
                <StatusBadge label="Activa" tone="success" />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  error={errors.companyName?.message}
                  label="Razon social"
                  placeholder="Centro Estetico Bella SpA"
                  {...register('companyName')}
                />
                <Input
                  error={errors.businessName?.message}
                  label="Nombre comercial"
                  placeholder="Centro Estetico Bella"
                  {...register('businessName')}
                />
                <Select
                  error={errors.timezone?.message}
                  label="Zona horaria"
                  options={timezoneOptions}
                  {...register('timezone')}
                />
                <Select
                  error={errors.currency?.message}
                  label="Moneda"
                  options={currencyOptions}
                  {...register('currency')}
                />
              </div>
            </Card>

            <Card className="space-y-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Vista operativa
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-slate-950">
                  Snapshot visible en el shell
                </h2>
              </div>
              <div className="rounded-[24px] border border-blue-100 bg-blue-50 px-5 py-5">
                <p className="text-sm font-semibold text-blue-800">Negocio seleccionado</p>
                <p className="mt-2 text-2xl font-semibold text-slate-950">
                  {companyQuery.data.businessName}
                </p>
                <p className="mt-2 text-sm leading-6 text-slate-700">
                  El topbar y la sesion del usuario se sincronizan con este nombre comercial despues
                  del guardado.
                </p>
              </div>
              <div className="rounded-[24px] border border-[var(--color-border)] bg-slate-50 px-5 py-5">
                <p className="text-sm font-semibold text-slate-900">Contexto operativo</p>
                <p className="mt-2 text-sm leading-6 text-slate-600">
                  Esta configuracion es la fuente visible para administracion, topbar y partes del
                  flujo comercial de Fase 1.
                </p>
              </div>
            </Card>
          </div>

          <Card className="space-y-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                Contacto y soporte
              </p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-950">Canales principales</h2>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <Input
                error={errors.contactEmail?.message}
                label="Correo de contacto"
                placeholder="admin@demo.cl"
                type="email"
                {...register('contactEmail')}
              />
              <Input
                error={errors.supportPhone?.message}
                label="Telefono de soporte"
                placeholder="+56955550100"
                {...register('supportPhone')}
              />
            </div>

            <Input
              error={errors.address?.message}
              label="Direccion"
              placeholder="Av. Providencia 2450, Santiago"
              {...register('address')}
            />

            <div className="flex flex-wrap justify-end gap-3 border-t border-[var(--color-border)] pt-5">
              <Button onClick={() => navigate('/admin')} variant="secondary">
                Cancelar
              </Button>
              <Button
                disabled={!isOnline || updateCompanyMutation.isPending || !isDirty}
                loading={updateCompanyMutation.isPending || isSubmitting}
                type="submit"
              >
                Guardar cambios
              </Button>
            </div>
          </Card>
        </form>
      ) : null}
    </section>
  )
}
