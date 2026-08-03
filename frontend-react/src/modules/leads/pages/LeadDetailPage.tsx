import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  addLeadNoteRequest,
  getLeadDetailRequest,
  updateLeadStageRequest,
} from '../../../services/api/leadsApi'
import {
  getLeadOriginLabel,
  getLeadStageLabel,
  getLeadStageTone,
  leadStageOptions,
} from '../leadOptions'

const noteSchema = z.object({
  noteText: z.string().trim().min(1, 'Escribe una nota antes de guardar.').max(2000),
})

type NoteValues = z.infer<typeof noteSchema>

export function LeadDetailPage() {
  const { prospectId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const noteForm = useForm<NoteValues>({
    resolver: zodResolver(noteSchema),
    defaultValues: { noteText: '' },
  })

  const leadQuery = useQuery({
    queryKey: ['leads', 'detail', prospectId],
    queryFn: () => getLeadDetailRequest(prospectId ?? ''),
    enabled: Boolean(prospectId),
    refetchInterval: isOnline ? 30_000 : false,
  })

  const stageMutation = useMutation({
    mutationFn: async (stage: string) => {
      if (!prospectId) {
        throw new Error('No hay prospecto seleccionado.')
      }
      return updateLeadStageRequest(prospectId, { stage })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['leads'] })
      showToast({
        title: 'Etapa actualizada',
        description: 'El cambio de etapa ya se refleja en el embudo.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo cambiar la etapa',
        description: 'Reintenta nuevamente en unos segundos.',
        tone: 'error',
      })
    },
  })

  const noteMutation = useMutation({
    mutationFn: async (noteText: string) => {
      if (!prospectId) {
        throw new Error('No hay prospecto seleccionado.')
      }
      return addLeadNoteRequest(prospectId, { noteText })
    },
    onSuccess: async () => {
      noteForm.reset({ noteText: '' })
      await queryClient.invalidateQueries({ queryKey: ['leads'] })
      showToast({
        title: 'Nota agregada',
        description: 'La nota ya aparece en el historial del prospecto.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo guardar la nota',
        description: 'Reintenta nuevamente con una nota mas corta o revisa tu conexion.',
        tone: 'error',
      })
    },
  })

  const onSubmitNote = noteForm.handleSubmit(async (values) => {
    await noteMutation.mutateAsync(values.noteText)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button onClick={() => navigate('/prospects')} variant="secondary">
              Volver a prospectos
            </Button>
            {leadQuery.data ? (
              <Button onClick={() => navigate(`/prospects/${leadQuery.data.id}/edit`)}>
                Editar prospecto
              </Button>
            ) : null}
          </>
        }
        description="Ficha comercial del prospecto con selector de etapa, datos de contacto, notas y acceso a la conversación asociada cuando existe."
        eyebrow="Detalle de prospecto"
        title="Prospecto"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Estado sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Puedes revisar la ficha, pero los cambios de etapa y nuevas notas quedaran bloqueados.
          </p>
        </Card>
      ) : null}

      {leadQuery.isPending ? (
        <LoadingState
          message="Cargando la ficha del prospecto, su etapa actual y el historial de notas."
          variant="detail"
        />
      ) : leadQuery.isError || !leadQuery.data ? (
        <ErrorState
          description="No pudimos recuperar el detalle del prospecto seleccionado."
          onRetry={() => void leadQuery.refetch()}
          title="No fue posible abrir el prospecto"
        />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
          <Card className="space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[var(--color-border)] pb-6">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                  {getLeadOriginLabel(leadQuery.data.sourceType)}
                </p>
                <h2 className="mt-2 text-[30px] font-semibold text-[var(--color-text)]">
                  {leadQuery.data.displayName}
                </h2>
                <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
                  {leadQuery.data.phone} · {leadQuery.data.email ?? 'Sin correo registrado'}
                </p>
              </div>

              <StatusBadge
                label={getLeadStageLabel(leadQuery.data.stage)}
                tone={getLeadStageTone(leadQuery.data.stage)}
              />
            </div>

            <Card className="space-y-4 bg-slate-50">
              <div className="flex flex-wrap items-end gap-4">
                <div className="min-w-[220px] flex-1">
                  <Select
                    defaultValue={leadQuery.data.stage}
                    label="Etapa actual"
                    options={leadStageOptions.map((option) => ({
                      label: option.label,
                      value: option.value,
                    }))}
                    onChange={(event) => void stageMutation.mutateAsync(event.target.value)}
                  />
                </div>

                <div className="rounded-[18px] border border-[var(--color-border)] bg-white px-4 py-3 text-sm text-slate-600">
                  Ultima actualizacion:{' '}
                  {dayjs(leadQuery.data.updatedAt).format('DD MMM YYYY HH:mm')}
                </div>
              </div>
            </Card>

            <div className="grid gap-4 md:grid-cols-2">
              <Card className="space-y-2 bg-slate-50">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Responsable
                </p>
                <p className="text-sm font-medium text-[var(--color-text)]">
                  {leadQuery.data.assignedUserName ?? 'Sin asignar'}
                </p>
              </Card>

              <Card className="space-y-2 bg-slate-50">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Sucursal
                </p>
                <p className="text-sm font-medium text-[var(--color-text)]">
                  {leadQuery.data.locationName ?? 'Sin sucursal asignada'}
                </p>
              </Card>

              <Card className="space-y-2 bg-slate-50">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Alta
                </p>
                <p className="text-sm font-medium text-[var(--color-text)]">
                  {dayjs(leadQuery.data.createdAt).format('DD MMM YYYY HH:mm')}
                </p>
              </Card>
            </div>

            <Card className="space-y-4 bg-slate-50">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                  Nota principal
                </p>
                <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
                  {leadQuery.data.notes ?? 'Sin nota principal registrada.'}
                </p>
              </div>
            </Card>

            <Card className="space-y-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                  Notas
                </p>
                <h3 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">
                  Historial comercial
                </h3>
              </div>

              <form className="space-y-4" onSubmit={onSubmitNote}>
                <Textarea
                  error={noteForm.formState.errors.noteText?.message}
                  label="Agregar nota"
                  placeholder="Escribe un seguimiento, acuerdo o contexto relevante."
                  rows={5}
                  {...noteForm.register('noteText')}
                />

                <div className="flex justify-end">
                  <Button disabled={!isOnline} loading={noteMutation.isPending} type="submit">
                    Guardar nota
                  </Button>
                </div>
              </form>

              {leadQuery.data.noteHistory.length === 0 ? (
                <EmptyState
                  description="Este prospecto aun no tiene notas registradas."
                  title="Sin historial de notas"
                  variant="card"
                />
              ) : (
                <div className="space-y-3">
                  {leadQuery.data.noteHistory.map((note) => (
                    <div
                      key={note.id}
                      className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 p-4"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm font-semibold text-[var(--color-text)]">
                          {note.authorUserName}
                        </p>
                        <span className="text-xs uppercase tracking-[0.16em] text-slate-500">
                          {dayjs(note.createdAt).format('DD MMM HH:mm')}
                        </span>
                      </div>
                      <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
                        {note.noteText}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </Card>

          <Card className="space-y-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                Atajos
              </p>
              <h3 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">
                Acciones relacionadas
              </h3>
            </div>

            {leadQuery.data.conversationId ? (
              <Link
                className={buttonClassName({ variant: 'secondary', fullWidth: true })}
                to={`/conversations/${leadQuery.data.conversationId}`}
              >
                Abrir conversacion vinculada
              </Link>
            ) : (
              <div className="rounded-[20px] border border-dashed border-[var(--color-border)] bg-slate-50 p-4 text-sm leading-6 text-[var(--color-text-secondary)]">
                Este prospecto fue creado manualmente y no tiene una conversacion asociada.
              </div>
            )}

            <Link
              className={buttonClassName({ variant: 'secondary', fullWidth: true })}
              to={`/prospects/${leadQuery.data.id}/appointments/new`}
            >
              Crear cita
            </Link>

            <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 p-4 text-sm leading-6 text-[var(--color-text-secondary)]">
              La etapa puede cambiarse directamente desde esta vista para validar el flujo del
              embudo.
            </div>
          </Card>
        </div>
      )}
    </section>
  )
}
