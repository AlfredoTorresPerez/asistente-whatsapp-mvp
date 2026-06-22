import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import {
  createAestheticService,
  getAestheticService,
  listAestheticServiceCategories,
  updateAestheticService,
} from '../../../services/api/aestheticApi'
import {
  createCatalogProduct,
  getCatalogProduct,
  listCatalogCategories,
  updateCatalogProduct,
} from '../../../services/api/catalogEtapa8Api'
import type {
  CatalogProductResponse,
  AestheticServiceResponse,
  UpsertCatalogProductRequest,
  UpsertAestheticServiceRequest,
} from '../../../services/api/types'

type CatalogItemType = 'service' | 'product'

type FormState = {
  active: boolean
  aftercareRecommendations: string
  availabilityRules: string
  bookingRules: string
  cancellationRules: string
  categoryCode: string
  code: string
  compatibleServices: string
  contraindications: string
  crossSellRules: string
  description: string
  durationMinutes: string
  expirationDate: string
  name: string
  price: string
  professionalRequired: string
  recommendationRules: string
  requiresInformedConsent: boolean
  requiresPriorEvaluation: boolean
  stock: string
  stockMinimum: string
  supplier: string
  supplies: string
  usageRestrictions: string
}

const emptyForm: FormState = {
  active: true,
  aftercareRecommendations: '',
  availabilityRules: '',
  bookingRules: '',
  cancellationRules: '',
  categoryCode: '',
  code: '',
  compatibleServices: '',
  contraindications: '',
  crossSellRules: '',
  description: '',
  durationMinutes: '60',
  expirationDate: '',
  name: '',
  price: '0',
  professionalRequired: '',
  recommendationRules: '',
  requiresInformedConsent: false,
  requiresPriorEvaluation: false,
  stock: '0',
  stockMinimum: '0',
  supplier: '',
  supplies: '',
  usageRestrictions: '',
}

const selectClassName =
  'h-12 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)] focus:ring-4 focus:ring-[var(--color-primary)]/12'

function nullable(value: string) {
  return value.trim() === '' ? null : value.trim()
}

function numberValue(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function fromService(service: AestheticServiceResponse): FormState {
  return {
    ...emptyForm,
    active: service.active,
    aftercareRecommendations: service.aftercareRecommendations ?? '',
    availabilityRules: service.availabilityRules ?? '',
    bookingRules: service.bookingRules ?? '',
    cancellationRules: service.cancellationRules ?? '',
    categoryCode: service.categoryCode,
    code: service.code,
    contraindications: service.contraindications ?? '',
    description: service.description,
    durationMinutes: String(service.durationMinutes),
    name: service.name,
    price: String(service.priceBase),
    professionalRequired: service.professionalRequired,
    requiresInformedConsent: service.requiresInformedConsent,
    requiresPriorEvaluation: service.requiresPriorEvaluation,
    supplies: service.supplies ?? '',
  }
}

function fromProduct(product: CatalogProductResponse): FormState {
  return {
    ...emptyForm,
    active: product.active,
    categoryCode: product.categoryCode,
    code: product.sku,
    description: product.description ?? '',
    expirationDate: product.expiresAt ?? '',
    name: product.name,
    price: String(product.price),
    stock: String(product.stock),
    stockMinimum: String(product.stockMinimum),
    supplier: product.supplier ?? '',
  }
}

export function CatalogFormPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { productId, serviceId } = useParams()
  const isEdit = Boolean(productId || serviceId)
  const initialType: CatalogItemType = location.pathname.includes('/services/') ? 'service' : 'product'
  const [itemType, setItemType] = useState<CatalogItemType>(initialType)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [formError, setFormError] = useState<string | null>(null)
  const [loadedEntityKey, setLoadedEntityKey] = useState('')

  const serviceCategoriesQuery = useQuery({
    queryKey: ['aesthetic', 'service-categories'],
    queryFn: () => listAestheticServiceCategories({ active: true, size: 100 }),
  })

  const productCategoriesQuery = useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: () => listCatalogCategories({ active: true, size: 100 }),
  })

  const serviceQuery = useQuery({
    enabled: Boolean(serviceId),
    queryKey: ['aesthetic', 'services', serviceId],
    queryFn: () => getAestheticService(serviceId ?? ''),
  })

  const productQuery = useQuery({
    enabled: Boolean(productId),
    queryKey: ['catalog', 'products', productId],
    queryFn: () => getCatalogProduct(productId ?? ''),
  })

  const loadedEntity = serviceQuery.data
    ? { form: fromService(serviceQuery.data), key: `service:${serviceQuery.data.id}`, type: 'service' as const }
    : productQuery.data
      ? { form: fromProduct(productQuery.data), key: `product:${productQuery.data.id}`, type: 'product' as const }
      : null

  if (loadedEntity && loadedEntity.key !== loadedEntityKey) {
    setLoadedEntityKey(loadedEntity.key)
    setItemType(loadedEntity.type)
    setForm(loadedEntity.form)
  }

  const categories = useMemo(() => itemType === 'service'
    ? serviceCategoriesQuery.data?.items ?? []
    : productCategoriesQuery.data?.items ?? [], [
      itemType,
      productCategoriesQuery.data?.items,
      serviceCategoriesQuery.data?.items,
    ])

  if (!isEdit && !form.categoryCode && categories.length > 0) {
    setForm((current) => ({ ...current, categoryCode: categories[0].code }))
  }

  const pageTitle = isEdit
    ? itemType === 'service' ? 'Editar servicio' : 'Editar producto'
    : itemType === 'service' ? 'Crear servicio' : 'Crear producto'

  const buildServiceRequest = (activeOverride = form.active): UpsertAestheticServiceRequest => ({
    active: activeOverride,
    aftercareRecommendations: nullable(form.aftercareRecommendations),
    availabilityRules: nullable(form.availabilityRules),
    bookingRules: nullable(form.bookingRules),
    cancellationRules: nullable(form.cancellationRules),
    categoryCode: form.categoryCode,
    code: nullable(form.code),
    contraindications: nullable(form.contraindications),
    description: form.description.trim(),
    durationMinutes: numberValue(form.durationMinutes),
    name: form.name.trim(),
    priceBase: numberValue(form.price),
    professionalRequired: form.professionalRequired.trim() || 'Profesional del centro',
    requiresInformedConsent: form.requiresInformedConsent,
    requiresPriorEvaluation: form.requiresPriorEvaluation,
    supplies: nullable(form.supplies),
  })

  const buildProductRequest = (): UpsertCatalogProductRequest => ({
    active: form.active,
    categoryCode: form.categoryCode,
    description: nullable(form.description),
    expiresAt: nullable(form.expirationDate),
    name: form.name.trim(),
    price: numberValue(form.price),
    sku: nullable(form.code),
    stock: numberValue(form.stock),
    stockMinimum: numberValue(form.stockMinimum),
    supplier: nullable(form.supplier),
  })

  const validateBaseForm = () => {
    if (!form.name.trim() || !form.description.trim() || !form.categoryCode) {
      throw new Error('Nombre, descripcion y categoria son obligatorios.')
    }
  }

  const mutation = useMutation({
    mutationFn: async () => {
      setFormError(null)
      validateBaseForm()
      if (itemType === 'service') {
        if (serviceId) {
          return updateAestheticService(serviceId, buildServiceRequest())
        }
        return createAestheticService(buildServiceRequest())
      }
      if (productId) {
        return updateCatalogProduct(productId, buildProductRequest())
      }
      return createCatalogProduct(buildProductRequest())
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'No fue posible guardar el catalogo.')
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['aesthetic'] })
      await queryClient.invalidateQueries({ queryKey: ['catalog'] })
      navigate('/catalog')
    },
  })

  const deactivateServiceMutation = useMutation({
    mutationFn: async () => {
      if (!serviceId) {
        throw new Error('No existe un servicio seleccionado para desactivar.')
      }
      setFormError(null)
      validateBaseForm()
      return updateAestheticService(serviceId, buildServiceRequest(false))
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'No fue posible desactivar el servicio.')
    },
    onSuccess: async () => {
      setForm((current) => ({ ...current, active: false }))
      await queryClient.invalidateQueries({ queryKey: ['aesthetic'] })
      await queryClient.invalidateQueries({ queryKey: ['catalog'] })
    },
  })

  const isLoading = serviceQuery.isPending && Boolean(serviceId) || productQuery.isPending && Boolean(productId)
  const hasLoadError = serviceQuery.isError || productQuery.isError

  const detailHint = useMemo(() => {
    if (itemType === 'service') {
      return 'Estos datos son usados por la IA para responder precios, duracion, contraindicaciones, disponibilidad y cuidados posteriores.'
    }
    return 'Estos datos son usados por la IA para responder consultas de stock, precio, recomendacion y venta cruzada.'
  }, [itemType])

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link className={buttonClassName({ variant: 'secondary' })} to="/catalog">
            Volver al catalogo
          </Link>
        }
        description="Formulario conectado al backend para crear o editar servicios y productos del centro estetico."
        eyebrow="Catalogo"
        title={pageTitle}
      />

      {isLoading ? <LoadingState message="Cargando informacion del item seleccionado." variant="page" /> : null}
      {hasLoadError ? (
        <ErrorState
          description="No fue posible cargar el item para editar. Verifica que exista y que el backend este disponible."
          onRetry={() => {
            void serviceQuery.refetch()
            void productQuery.refetch()
          }}
          title="No fue posible cargar el item"
        />
      ) : null}

      {!isLoading && !hasLoadError ? (
        <Card className="space-y-6">
          <div className="flex flex-col gap-3 border-b border-[var(--color-border)] pb-5 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--color-text-secondary)]">
                Item editable
              </p>
              <h2 className="mt-2 text-xl font-semibold text-[var(--color-text)]">{pageTitle}</h2>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-[var(--color-text-secondary)]">{detailHint}</p>
            </div>
            <StatusBadge label={form.active ? 'Activo' : 'Desactivado'} tone={form.active ? 'success' : 'warning'} />
          </div>

          {formError ? (
            <div className="rounded-[18px] border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-800">
              {formError}
            </div>
          ) : null}

          <form
            className="space-y-6"
            onSubmit={(event) => {
              event.preventDefault()
              mutation.mutate()
            }}
          >
            {!isEdit ? (
              <label className="block">
                <span className="mb-2.5 block text-sm font-medium text-[#23385F]">Tipo de item</span>
                <select
                  className={selectClassName}
                  onChange={(event) => {
                    setItemType(event.target.value as CatalogItemType)
                    setForm({ ...emptyForm, active: form.active })
                  }}
                  value={itemType}
                >
                  <option value="service">Servicio</option>
                  <option value="product">Producto</option>
                </select>
              </label>
            ) : null}

            <div className="grid gap-4 md:grid-cols-2">
              <Input label="Nombre" onChange={(event) => setForm({ ...form, name: event.target.value })} value={form.name} />
              <Input label="Codigo" hint="Si queda vacio se genera desde el nombre." onChange={(event) => setForm({ ...form, code: event.target.value })} value={form.code} />
              <label className="block">
                <span className="mb-2.5 block text-sm font-medium text-[#23385F]">Categoria</span>
                <select className={selectClassName} onChange={(event) => setForm({ ...form, categoryCode: event.target.value })} value={form.categoryCode}>
                  <option value="">Seleccionar categoria</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.code}>{category.name}</option>
                  ))}
                </select>
              </label>
              <Input label="Precio" min="0" onChange={(event) => setForm({ ...form, price: event.target.value })} step="1" type="number" value={form.price} />
            </div>

            <Textarea label="Descripcion" onChange={(event) => setForm({ ...form, description: event.target.value })} value={form.description} />

            {itemType === 'service' ? (
              <div className="space-y-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <Input label="Duracion en minutos" min="10" onChange={(event) => setForm({ ...form, durationMinutes: event.target.value })} type="number" value={form.durationMinutes} />
                  <Input label="Profesional requerido" onChange={(event) => setForm({ ...form, professionalRequired: event.target.value })} value={form.professionalRequired} />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <Textarea label="Insumos asociados" onChange={(event) => setForm({ ...form, supplies: event.target.value })} value={form.supplies} />
                  <Textarea label="Contraindicaciones" onChange={(event) => setForm({ ...form, contraindications: event.target.value })} value={form.contraindications} />
                  <Textarea label="Reglas de disponibilidad" onChange={(event) => setForm({ ...form, availabilityRules: event.target.value })} value={form.availabilityRules} />
                  <Textarea label="Reglas de reserva" onChange={(event) => setForm({ ...form, bookingRules: event.target.value })} value={form.bookingRules} />
                  <Textarea label="Reglas de cancelacion" onChange={(event) => setForm({ ...form, cancellationRules: event.target.value })} value={form.cancellationRules} />
                  <Textarea label="Cuidados posteriores" onChange={(event) => setForm({ ...form, aftercareRecommendations: event.target.value })} value={form.aftercareRecommendations} />
                </div>
                <div className="grid gap-4 md:grid-cols-3">
                  <CheckboxField checked={form.requiresPriorEvaluation} label="Requiere evaluacion previa" onChange={(checked) => setForm({ ...form, requiresPriorEvaluation: checked })} />
                  <CheckboxField checked={form.requiresInformedConsent} label="Requiere consentimiento informado" onChange={(checked) => setForm({ ...form, requiresInformedConsent: checked })} />
                  <CheckboxField checked={form.active} label="Servicio activo" onChange={(checked) => setForm({ ...form, active: checked })} />
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <Input label="Stock" min="0" onChange={(event) => setForm({ ...form, stock: event.target.value })} type="number" value={form.stock} />
                  <Input label="Stock minimo" min="0" onChange={(event) => setForm({ ...form, stockMinimum: event.target.value })} type="number" value={form.stockMinimum} />
                  <Input label="Proveedor" onChange={(event) => setForm({ ...form, supplier: event.target.value })} value={form.supplier} />
                  <Input label="Fecha de vencimiento" onChange={(event) => setForm({ ...form, expirationDate: event.target.value })} type="date" value={form.expirationDate} />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <Textarea label="Servicios compatibles" onChange={(event) => setForm({ ...form, compatibleServices: event.target.value })} value={form.compatibleServices} />
                  <Textarea label="Reglas de recomendacion" onChange={(event) => setForm({ ...form, recommendationRules: event.target.value })} value={form.recommendationRules} />
                  <Textarea label="Reglas de venta cruzada" onChange={(event) => setForm({ ...form, crossSellRules: event.target.value })} value={form.crossSellRules} />
                  <Textarea label="Restricciones de uso" onChange={(event) => setForm({ ...form, usageRestrictions: event.target.value })} value={form.usageRestrictions} />
                </div>
                <CheckboxField checked={form.active} label="Producto activo" onChange={(checked) => setForm({ ...form, active: checked })} />
              </div>
            )}

            <div className="flex flex-wrap justify-between gap-3 border-t border-[var(--color-border)] pt-5">
              <div>
                {isEdit && itemType === 'service' && form.active ? (
                  <Button
                    loading={deactivateServiceMutation.isPending}
                    onClick={() => deactivateServiceMutation.mutate()}
                    type="button"
                    variant="danger"
                  >
                    Desactivar servicio por falta de cobertura
                  </Button>
                ) : null}
              </div>
              <div className="flex flex-wrap justify-end gap-3">
                <Link className={buttonClassName({ variant: 'secondary' })} to="/catalog">Cancelar</Link>
                <Button loading={mutation.isPending} type="submit">Guardar cambios</Button>
              </div>
            </div>
          </form>
        </Card>
      ) : null}
    </section>
  )
}

function CheckboxField({ checked, label, onChange }: { checked: boolean; label: string; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex items-center gap-3 rounded-[18px] border border-[var(--color-border)] bg-white p-4 text-sm font-semibold text-[var(--color-text)]">
      <input checked={checked} className="h-4 w-4" onChange={(event) => onChange(event.target.checked)} type="checkbox" />
      {label}
    </label>
  )
}
