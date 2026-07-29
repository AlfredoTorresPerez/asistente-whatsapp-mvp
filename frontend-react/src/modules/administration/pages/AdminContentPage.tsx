import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Modal } from '../../../components/overlay/Modal'
import { ApiClientError } from '../../../services/api/httpClient'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Textarea } from '../../../components/ui/Textarea'
import { Select } from '../../../components/ui/Select'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { ImageUpload } from '../../../components/ui/ImageUpload'
import { useToast } from '../../../lib/toast'
import {
  createContentItemRequest,
  deleteContentItemImageRequest,
  deleteContentItemRequest,
  getContentItemsRequest,
  getContentItemRequest,
  updateContentItemRequest,
  updateContentItemStatusRequest,
  uploadContentItemImageRequest,
} from '../../../services/api/contentApi'
import type {
  ContentItemDetailResponse,
  ContentItemListRequest,
  ContentItemSummaryResponse,
  CreateContentItemRequest,
  UpdateContentItemRequest,
  UpdateContentItemStatusRequest,
  PublicContentItemResponse,
  ContentStatsResponse,
} from '../../../services/api/types'

type ContentType = 'CATEGORY' | 'SERVICE' | 'LANDING_PAGE'
type ContentStatus = 'ACTIVE' | 'INACTIVE'

const CONTENT_TYPES: { value: ContentType; label: string }[] = [
  { value: 'CATEGORY', label: 'Categoría' },
  { value: 'SERVICE', label: 'Servicio' },
  { value: 'LANDING_PAGE', label: 'Landing page' },
]

const CONTENT_STATUSES: { value: ContentStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'Inactivo' },
]

const TYPE_OPTIONS = CONTENT_TYPES.map((t) => ({ label: t.label, value: t.value }))
const STATUS_OPTIONS = CONTENT_STATUSES.map((s) => ({ label: s.label, value: s.value }))

type FormState = {
  id: string | null
  type: ContentType | ''
  text: string
  status: ContentStatus | ''
  imageFile: File | null
  imagePreview: string | null
}

type FormErrors = Partial<Record<keyof FormState, string>> & {
  general?: string
}

const emptyForm: FormState = {
  id: null,
  type: '',
  text: '',
  status: 'ACTIVE',
  imageFile: null,
  imagePreview: null,
}

export function AdminContentPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [form, setForm] = useState<FormState>(emptyForm)
  const [formErrors, setFormErrors] = useState<FormErrors>({})
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState<ContentType | 'ALL'>('ALL')
  const [statusFilter, setStatusFilter] = useState<ContentStatus | 'ALL'>('ALL')
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [viewMode, setViewMode] = useState<'table' | 'form'>('table')
  const [selectedItem, setSelectedItem] = useState<ContentItemSummaryResponse | null>(null)

  const filters: ContentItemListRequest = {
    page,
    size,
    search: search || undefined,
    type: typeFilter === 'ALL' ? undefined : typeFilter,
    status: statusFilter === 'ALL' ? undefined : statusFilter,
  }

  const listQuery = useQuery({
    queryKey: ['content-items', filters],
    queryFn: () => getContentItemsRequest(filters),
  })

  const items = useMemo(() => listQuery.data?.items ?? [], [listQuery.data])
  const stats = listQuery.data?.stats

  const createMutation = useMutation({
    mutationFn: async (payload: { request: CreateContentItemRequest; image?: File }) => {
      return createContentItemRequest(payload.request, payload.image)
    },
    onSuccess: (data) => {
      showToast({
        title: 'Contenido creado',
        description: 'El registro se guardó correctamente.',
        tone: 'success',
      })
      setFormErrors({})
      queryClient.invalidateQueries({ queryKey: ['content-items'] })
      setForm({ ...emptyForm, status: 'ACTIVE' })
      setViewMode('table')
    },
    onError: handleMutationError,
  })

  const updateMutation = useMutation({
    mutationFn: async (payload: {
      id: string
      request: UpdateContentItemRequest
      image?: File
    }) => {
      const result = await updateContentItemRequest(payload.id, payload.request)
      if (payload.image) {
        await uploadContentItemImageRequest(payload.id, payload.image)
        await queryClient.invalidateQueries({ queryKey: ['content-item', payload.id] })
      }
      return result
    },
    onSuccess: () => {
      showToast({
        title: 'Contenido actualizado',
        description: 'Los cambios se guardaron correctamente.',
        tone: 'success',
      })
      setFormErrors({})
      queryClient.invalidateQueries({ queryKey: ['content-items'] })
      setViewMode('table')
    },
    onError: handleMutationError,
  })

  const statusMutation = useMutation({
    mutationFn: async (payload: { id: string; status: ContentStatus }) => {
      return updateContentItemStatusRequest(payload.id, { status: payload.status })
    },
    onSuccess: () => {
      showToast({
        title: 'Estado actualizado',
        tone: 'success',
      })
      queryClient.invalidateQueries({ queryKey: ['content-items'] })
    },
    onError: (error) => {
      showToast({
        title: 'No se pudo actualizar el estado',
        description: error instanceof Error ? error.message : 'Intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  const deleteImageMutation = useMutation({
    mutationFn: deleteContentItemImageRequest,
    onSuccess: () => {
      showToast({ title: 'Imagen eliminada', tone: 'success' })
      queryClient.invalidateQueries({ queryKey: ['content-items'] })
      if (form.id) {
        getContentItemRequest(form.id).then((data) => {
          setForm((prev) => ({ ...prev, imagePreview: data.imageUrl }))
        })
      }
    },
    onError: () => {
      showToast({ title: 'No se pudo eliminar la imagen', tone: 'error' })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteContentItemRequest,
    onSuccess: () => {
      showToast({
        title: 'Contenido eliminado',
        description: 'El registro y su imagen asociada se eliminaron.',
        tone: 'success',
      })
      queryClient.invalidateQueries({ queryKey: ['content-items'] })
      setForm(emptyForm)
    },
    onError: () => {
      showToast({ title: 'No se pudo eliminar', tone: 'error' })
    },
  })

  const detailQuery = useQuery({
    queryKey: ['content-item', form.id],
    queryFn: () => getContentItemRequest(form.id!),
    enabled: !!form.id && viewMode === 'form',
  })

  useEffect(() => {
    if (detailQuery.data) {
      const data = detailQuery.data
      setForm({
        id: data.id,
        type: data.type as ContentType,
        text: data.text,
        status: data.status as ContentStatus,
        imageFile: null,
        imagePreview: data.imageUrl,
      })
    }
  }, [detailQuery.data])

  function handleMutationError(error: unknown) {
    if (error instanceof ApiClientError) {
      if (error.status === 401) {
        setFormErrors({ general: 'Tu sesión expiro. Inicia sesión nuevamente.' })
        showToast({ title: 'Sesión expirada', tone: 'error' })
        navigate('/login', { replace: true })
        return
      }
      const nextErrors = error.fieldErrors ? toFormErrors(error.fieldErrors) : {}
      setFormErrors({
        ...nextErrors,
        general:
          Object.keys(nextErrors).length > 0
            ? 'Corrige los campos marcados antes de guardar.'
            : error.message,
      })
      showToast({
        title: 'No se pudo guardar',
        description:
          Object.keys(nextErrors).length > 0 ? 'Hay campos con datos inválidos.' : error.message,
        tone: 'error',
      })
      return
    }
    setFormErrors({ general: 'No fue posible guardar. Intenta nuevamente.' })
    showToast({ title: 'No se pudo guardar', tone: 'error' })
  }

  function toFormErrors(fieldErrors: Record<string, string>): FormErrors {
    const errors: FormErrors = {}
    const supportedFields = new Set<keyof FormState>(['type', 'text', 'status'])
    Object.entries(fieldErrors).forEach(([field, message]) => {
      if (supportedFields.has(field as keyof FormState)) {
        errors[field as keyof FormState] = message
      }
    })
    return errors
  }

  const updateField = useCallback(
    (field: keyof FormState | 'imageFile' | 'imagePreview', value: string | File | null) => {
      setForm((current) => {
        const next = { ...current, [field]: value }
        if (field === 'imageFile' && value instanceof File) {
          next.imagePreview = URL.createObjectURL(value)
        } else if (field === 'imageFile' && value === null) {
          next.imagePreview = null
        }
        return next
      })
      setFormErrors((current) => {
        const { [field]: _, general: __, ...remaining } = current
        return remaining
      })
    },
    [],
  )

  const validateForm = useCallback((): FormErrors => {
    const errors: FormErrors = {}
    if (!form.type) errors.type = 'El tipo es obligatorio.'
    if (!form.text.trim()) errors.text = 'El texto es obligatorio.'
    else if (form.text.trim().length > 200)
      errors.text = 'El texto no puede superar 200 caracteres.'
    if (!form.status) errors.status = 'El estado es obligatorio.'
    return errors
  }, [form])

  const submit = useCallback(
    (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault()
      const errors = validateForm()
      setFormErrors(errors)
      if (Object.keys(errors).length > 0) {
        showToast({
          title: 'Revisa el formulario',
          description: 'Corrige los campos marcados.',
          tone: 'error',
        })
        return
      }
      const payload: CreateContentItemRequest | UpdateContentItemRequest = {
        type: form.type,
        text: form.text.trim(),
        status: form.status,
      }
      if (form.id) {
        updateMutation.mutate({
          id: form.id,
          request: payload as UpdateContentItemRequest,
          image: form.imageFile ?? undefined,
        })
      } else {
        createMutation.mutate({
          request: payload as CreateContentItemRequest,
          image: form.imageFile ?? undefined,
        })
      }
    },
    [form, validateForm, createMutation, updateMutation, showToast],
  )

  const startNew = useCallback(() => {
    setForm({ ...emptyForm, imageFile: null, imagePreview: null })
    setFormErrors({})
    setViewMode('form')
  }, [])

  const editItem = useCallback((item: ContentItemSummaryResponse) => {
    setFormErrors({})
    setForm((prev) => ({ ...prev, id: item.id }))
    setViewMode('form')
  }, [])

  const toggleStatus = useCallback(
    (item: ContentItemSummaryResponse) => {
      const newStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      statusMutation.mutate({ id: item.id, status: newStatus })
    },
    [statusMutation],
  )

  const confirmDelete = useCallback((item: ContentItemSummaryResponse) => {
    setSelectedItem(item)
  }, [])

  const executeDelete = useCallback(() => {
    if (selectedItem) {
      deleteMutation.mutate(selectedItem.id)
      setSelectedItem(null)
    }
  }, [selectedItem, deleteMutation])

  const filteredItems = useMemo(() => {
    return items
  }, [items])

  return (
    <section className="space-y-6">
      <PageHeader
        actions={<Button onClick={startNew}>Nuevo registro</Button>}
        description="Administra imágenes y textos utilizados en categorías, servicios y landing page."
        eyebrow="Administración"
        title="Contenido visual"
      />

      {listQuery.isPending ? (
        <LoadingState message="Cargando contenido visual." variant="page" />
      ) : listQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar el contenido visual."
          onRetry={() => void listQuery.refetch()}
          title="No fue posible cargar el contenido"
        />
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              label="Total de registros"
              value={String(stats?.total ?? 0)}
              helper="En esta empresa"
              icon="\uD83D\uDCC4"
            />
            <MetricCard
              label="Activos"
              value={String(stats?.active ?? 0)}
              helper="Visibles en público"
              icon="\u2705"
            />
            <MetricCard
              label="Inactivos"
              value={String(stats?.inactive ?? 0)}
              helper="Borradores u ocultos"
              icon="\u23F8\uFE0F"
            />
            <MetricCard
              label="Sin imagen"
              value={String(stats?.withoutImage ?? 0)}
              helper="Pendientes de imagen"
              icon="\uD83D\uDDBC\uFE0F"
            />
          </div>

          <Card className="space-y-5">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
              <div>
                <h2 className="text-xl font-semibold text-slate-950">Registros</h2>
                <p className="mt-1 text-sm text-slate-500">
                  Filtra, busca y gestiona el contenido visual del negocio.
                </p>
              </div>
              <div className="flex flex-col gap-3 md:flex-row">
                <Input
                  className="min-w-[280px]"
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Buscar por texto..."
                  type="search"
                  value={search}
                />
                <Select
                  className="w-[180px]"
                  onChange={(e) => setTypeFilter(e.target.value as ContentType | 'ALL')}
                  value={typeFilter}
                  options={[{ value: 'ALL', label: 'Tipo: Todos' }, ...TYPE_OPTIONS]}
                />
                <Select
                  className="w-[180px]"
                  onChange={(e) => setStatusFilter(e.target.value as ContentStatus | 'ALL')}
                  value={statusFilter}
                  options={[
                    { value: 'ALL', label: 'Estado: Todos' },
                    { value: 'ACTIVE', label: 'Estado: Activos' },
                    { value: 'INACTIVE', label: 'Estado: Inactivos' },
                  ]}
                />
                {(search || typeFilter !== 'ALL' || statusFilter !== 'ALL') && (
                  <Button
                    onClick={() => {
                      setSearch('')
                      setTypeFilter('ALL')
                      setStatusFilter('ALL')
                    }}
                    variant="secondary"
                  >
                    Limpiar filtros
                  </Button>
                )}
              </div>
            </div>

            <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
              <div className="hidden grid-cols-[70px_minmax(100px,1fr)_minmax(140px,1fr)_80px_80px_180px] items-center border-b border-slate-200 bg-slate-50 px-4 py-3 text-xs font-bold uppercase tracking-[0.14em] text-slate-500 xl:grid">
                <span>Imagen</span>
                <span>Tipo</span>
                <span>Texto</span>
                <span>Estado</span>
                <span>Actualizado</span>
                <span>Acciones</span>
              </div>

              {filteredItems.map((item) => (
                <div
                  key={item.id}
                  className="grid gap-2 border-b border-slate-200 px-4 py-3 last:border-b-0 xl:grid-cols-[70px_minmax(100px,1fr)_minmax(140px,1fr)_80px_80px_180px] xl:items-center"
                >
                  <div className="relative h-16 w-16 xl:h-16 xl:w-16">
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt=""
                        className="h-full w-full object-cover rounded-lg"
                      />
                    ) : (
                      <div className="h-full w-full flex items-center justify-center rounded-lg bg-slate-100">
                        <svg
                          className="h-6 w-6 text-slate-400"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={1.5}
                            d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
                          />
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={1.5}
                            d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
                          />
                        </svg>
                      </div>
                    )}
                  </div>
                  <span className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                    {CONTENT_TYPES.find(
                      (t) => t.value === item.typeLabel.toUpperCase().replace(' ', '_'),
                    )?.label ?? item.typeLabel}
                  </span>
                  <p className="truncate text-sm text-slate-600 max-w-xs" title={item.textPreview}>
                    {item.textPreview}
                  </p>
                  <StatusBadge
                    label={item.status}
                    tone={item.status === 'ACTIVE' ? 'success' : 'neutral'}
                  />
                  <span className="text-sm text-slate-600 hidden sm:block">
                    {new Date(item.updatedAt).toLocaleDateString('es-CL', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                  <div className="flex shrink-0 gap-1.5">
                    <Button
                      className="px-2.5 text-xs"
                      onClick={() => editItem(item)}
                      size="sm"
                      variant="secondary"
                    >
                      Editar
                    </Button>
                    <Button
                      className="px-2.5 text-xs"
                      onClick={() => toggleStatus(item)}
                      size="sm"
                      variant="secondary"
                    >
                      {item.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
                    </Button>
                    <Button
                      className="px-2.5 text-xs"
                      onClick={() => confirmDelete(item)}
                      size="sm"
                      variant="secondary"
                    >
                      Eliminar
                    </Button>
                  </div>
                </div>
              ))}

              {filteredItems.length === 0 && (
                <p className="px-4 py-8 text-center text-sm text-slate-500">
                  No hay registros que coincidan con los filtros seleccionados.
                </p>
              )}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600">
              <span>
                Mostrando {filteredItems.length} de {stats?.total ?? 0} registros.
              </span>
              <div className="flex gap-2">
                <Button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  size="sm"
                  variant="secondary"
                >
                  Anterior
                </Button>
                <Button
                  disabled={
                    filteredItems.length < size || (stats && (page + 1) * size >= stats.total)
                  }
                  onClick={() => setPage((p) => p + 1)}
                  size="sm"
                  variant="secondary"
                >
                  Siguiente
                </Button>
              </div>
            </div>
          </Card>

          <Card className="space-y-5" id="content-form">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                {form.id ? 'Editar contenido' : 'Nuevo contenido'}
              </p>
              <h2 className="mt-2 text-xl font-semibold text-slate-950">
                {form.id ? 'Datos del registro' : 'Crear nuevo registro'}
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Selecciona el tipo, escribe el texto (máx. 200 caracteres), elige una imagen
                opcional y define el estado.
              </p>
            </div>

            <form className="space-y-4" onSubmit={submit}>
              {formErrors.general && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800">
                  {formErrors.general}
                </div>
              )}

              <div className="grid gap-4 md:grid-cols-2">
                <Select
                  error={formErrors.type}
                  label="Tipo"
                  onChange={(e) => updateField('type', e.target.value as ContentType)}
                  options={[{ value: '', label: 'Selecciona un tipo' }, ...TYPE_OPTIONS]}
                  value={form.type || ''}
                />
                <Select
                  error={formErrors.status}
                  label="Estado"
                  onChange={(e) => updateField('status', e.target.value as ContentStatus)}
                  options={STATUS_OPTIONS}
                  value={form.status}
                />
              </div>

              <div className="space-y-2">
                <label className="block text-sm font-semibold text-slate-700">Imagen</label>
                <ImageUpload
                  disabled={createMutation.isPending || updateMutation.isPending}
                  onChange={(file) => updateField('imageFile', file)}
                  onRemove={() => {
                    updateField('imageFile', null)
                    updateField('imagePreview', null)
                  }}
                  value={form.imagePreview}
                />
                <p className="text-xs text-slate-500">
                  PNG, JPG, WebP · Máx. 5 MB · Se guardará al guardar el registro
                </p>
              </div>

              <Textarea
                error={formErrors.text}
                label="Texto"
                maxLength={200}
                onChange={(e) => updateField('text', e.target.value)}
                placeholder="Escribe el texto descriptivo (máx. 200 caracteres)"
                value={form.text}
              />

              <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-slate-200">
                <Button onClick={() => setViewMode('table')} type="button" variant="secondary">
                  Cancelar
                </Button>
                <Button
                  loading={createMutation.isPending || updateMutation.isPending}
                  type="submit"
                >
                  {form.id ? 'Guardar cambios' : 'Crear registro'}
                </Button>
              </div>
            </form>
          </Card>
        </>
      )}

      <Modal
        maxWidthClassName="max-w-md"
        onClose={() => setSelectedItem(null)}
        open={Boolean(selectedItem)}
      >
        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                Eliminar contenido
              </p>
              <h3 className="mt-1 text-lg font-semibold text-slate-950">
                ¿Eliminar "{selectedItem?.textPreview}"?
              </h3>
            </div>
            <button
              className="inline-flex h-8 w-8 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              onClick={() => setSelectedItem(null)}
              type="button"
            >
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>

          <p className="text-sm text-slate-600">
            Esta acción eliminará el registro permanentemente. Si tiene una imagen asociada, también
            se eliminará del almacenamiento.
          </p>

          <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-slate-200">
            <Button onClick={() => setSelectedItem(null)} type="button" variant="secondary">
              Cancelar
            </Button>
            <Button loading={deleteMutation.isPending} onClick={executeDelete} variant="danger">
              Eliminar
            </Button>
          </div>
        </div>
      </Modal>
    </section>
  )
}

function MetricCard({
  helper,
  icon,
  label,
  value,
}: {
  helper: string
  icon: string
  label: string
  value: string
}) {
  return (
    <Card className="flex items-center gap-4">
      <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-2xl">
        {icon}
      </span>
      <span>
        <span className="block text-2xl font-semibold text-slate-950">{value}</span>
        <span className="block text-sm font-semibold text-slate-700">{label}</span>
        <span className="mt-1 block text-xs text-slate-500">{helper}</span>
      </span>
    </Card>
  )
}
