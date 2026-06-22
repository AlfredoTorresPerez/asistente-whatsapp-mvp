import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { FilterBar } from '../../../components/ui/FilterBar'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { formatEstadoRegistro, getRegistroTone, isRegistroActivo } from '../../../lib/statusFormatters'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  listAestheticServiceCategories,
  listAestheticServices,
  updateAestheticService,
} from '../../../services/api/aestheticApi'
import {
  listCatalogCategories,
  listCatalogProducts,
  updateCatalogProductStatus,
} from '../../../services/api/catalogEtapa8Api'
import type { CatalogProductResponse, AestheticServiceResponse } from '../../../services/api/types'

type CatalogTab = 'services' | 'products'
type ActiveFilter = '' | 'true' | 'false'

type CatalogRow = {
  category: string
  code: string
  detail: string
  href: string
  id: string
  name: string
  price: number
  status: boolean
  source: AestheticServiceResponse | CatalogProductResponse
  type: 'Servicio' | 'Producto' | 'Producto · stock bajo'
}

const fieldClassName =
  'h-11 w-full rounded-2xl border border-[var(--color-border)] bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const PAGE_SIZE = 10

const formatMoney = (value: number) =>
  new Intl.NumberFormat('es-CL', {
    currency: 'CLP',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)

function serviceToRow(service: AestheticServiceResponse): CatalogRow {
  return {
    category: service.categoryName,
    code: service.code,
    detail: `${service.durationMinutes} min · ${service.professionalRequired}`,
    href: `/catalog/services/${service.id}/edit`,
    id: service.id,
    name: service.name,
    price: service.priceBase,
    source: service,
    status: service.active,
    type: 'Servicio',
  }
}

function productToRow(product: CatalogProductResponse): CatalogRow {
  return {
    category: product.categoryName,
    code: product.sku,
    detail: `Stock ${product.stock} · mínimo ${product.stockMinimum}`,
    href: `/catalog/products/${product.id}/edit`,
    id: product.id,
    name: product.name,
    price: product.price,
    source: product,
    status: product.active,
    type: product.lowStock ? 'Producto · stock bajo' : 'Producto',
  }
}

function isServiceRow(row: CatalogRow): row is CatalogRow & { source: AestheticServiceResponse } {
  return row.type === 'Servicio'
}

function buildServiceStatusRequest(service: AestheticServiceResponse, active: boolean) {
  return {
    active,
    aftercareRecommendations: service.aftercareRecommendations,
    availabilityRules: service.availabilityRules,
    bookingRules: service.bookingRules,
    cancellationRules: service.cancellationRules,
    categoryCode: service.categoryCode,
    code: service.code,
    contraindications: service.contraindications,
    description: service.description,
    durationMinutes: service.durationMinutes,
    name: service.name,
    priceBase: service.priceBase,
    professionalRequired: service.professionalRequired,
    requiresInformedConsent: service.requiresInformedConsent,
    requiresPriorEvaluation: service.requiresPriorEvaluation,
    supplies: service.supplies,
  }
}

export function CatalogPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isOnline = useOnlineStatus()
  const [tab, setTab] = useState<CatalogTab>('services')
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [activeInput, setActiveInput] = useState<ActiveFilter>('')
  const [categoryInput, setCategoryInput] = useState('')
  const [filters, setFilters] = useState({ search: '', active: '' as ActiveFilter, categoryCode: '' })
  const [rowToToggle, setRowToToggle] = useState<{ row: CatalogRow; active: boolean } | null>(null)
  const [inlineError, setInlineError] = useState<string | null>(null)

  const servicesQuery = useQuery({
    queryKey: ['aesthetic', 'services', page, filters],
    queryFn: () =>
      listAestheticServices({
        active: filters.active === '' ? undefined : filters.active === 'true',
        categoryCode: filters.categoryCode || undefined,
        page,
        search: filters.search || undefined,
        size: PAGE_SIZE,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: isOnline ? 30_000 : false,
  })

  const productsQuery = useQuery({
    queryKey: ['catalog', 'products', page, filters],
    queryFn: () =>
      listCatalogProducts({
        active: filters.active === '' ? undefined : filters.active === 'true',
        categoryCode: filters.categoryCode || undefined,
        page,
        search: filters.search || undefined,
        size: PAGE_SIZE,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: isOnline ? 30_000 : false,
  })

  const serviceCategoriesQuery = useQuery({
    queryKey: ['aesthetic', 'service-categories'],
    queryFn: () => listAestheticServiceCategories({ active: true, size: 100 }),
  })

  const productCategoriesQuery = useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: () => listCatalogCategories({ active: true, size: 100 }),
  })

  const statusMutation = useMutation({
    mutationFn: async ({ active, row }: { row: CatalogRow; active: boolean }) => {
      if (isServiceRow(row)) {
        const service = row.source
        return updateAestheticService(service.id, buildServiceStatusRequest(service, active))
      }

      return updateCatalogProductStatus(row.source.id, active)
    },
    onError: (error) => {
      setInlineError(error instanceof Error ? error.message : 'No fue posible actualizar el estado del item del catálogo.')
    },
    onSuccess: async () => {
      setInlineError(null)
      setRowToToggle(null)
      await queryClient.invalidateQueries({ queryKey: ['aesthetic'] })
      await queryClient.invalidateQueries({ queryKey: ['catalog'] })
    },
  })

  const services = useMemo(() => servicesQuery.data?.items ?? [], [servicesQuery.data?.items])
  const products = useMemo(() => productsQuery.data?.items ?? [], [productsQuery.data?.items])
  const currentQuery = tab === 'services' ? servicesQuery : productsQuery
  const currentItems = tab === 'services' ? services.map(serviceToRow) : products.map(productToRow)
  const categories = tab === 'services'
    ? serviceCategoriesQuery.data?.items ?? []
    : productCategoriesQuery.data?.items ?? []

  const metrics = useMemo(() => {
    const activeServices = services.filter((service) => service.active).length
    const activeProducts = products.filter((product) => product.active).length
    const lowStock = products.filter((product) => product.lowStock).length
    const totalCategories = new Set([
      ...services.map((service) => service.categoryCode),
      ...products.map((product) => product.categoryCode),
    ]).size
    return { activeProducts, activeServices, lowStock, totalCategories }
  }, [products, services])

  const applyFilters = () => {
    setPage(0)
    setFilters({ active: activeInput, categoryCode: categoryInput, search: searchInput })
  }

  const clearFilters = () => {
    setPage(0)
    setSearchInput('')
    setActiveInput('')
    setCategoryInput('')
    setFilters({ active: '', categoryCode: '', search: '' })
  }

  const changeTab = (nextTab: CatalogTab) => {
    setTab(nextTab)
    setPage(0)
    setCategoryInput('')
    setFilters((current) => ({ ...current, categoryCode: '' }))
  }

  return (
    <section className="space-y-4 overflow-hidden">
      <PageHeader
        actions={
          <>
            <Link className={buttonClassName({ variant: 'secondary' })} to="/catalog/services/new">
              Crear servicio
            </Link>
            <Button onClick={() => navigate('/catalog/products/new')}>Crear producto</Button>
          </>
        }
        description="Catálogo real del centro estético conectado al servidor, con servicios, productos, precios, stock, categorías y estados editables."
        eyebrow="Catálogo"
        title="Catálogo de servicios y productos"
      />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Servicios activos" value={String(metrics.activeServices)} tone="success" />
        <MetricCard label="Productos activos" value={String(metrics.activeProducts)} tone="success" />
        <MetricCard label="Categorías visibles" value={String(metrics.totalCategories)} tone="info" />
        <MetricCard label="Stock bajo" value={String(metrics.lowStock)} tone={metrics.lowStock > 0 ? 'warning' : 'success'} />
      </div>

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50 p-4">
          <p className="text-sm font-semibold text-amber-900">Estado sin conexión</p>
          <p className="mt-1 text-sm leading-6 text-amber-800">
            Puedes ver datos cacheados, pero la edición se sincronizará solo cuando vuelva la conexión.
          </p>
        </Card>
      ) : null}

      <Card className="space-y-4 p-4 lg:p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex rounded-[18px] border border-[var(--color-border)] bg-slate-50 p-1">
            <button
              className={`rounded-[14px] px-4 py-2 text-sm font-semibold ${tab === 'services' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600'}`}
              onClick={() => changeTab('services')}
              type="button"
            >
              Servicios
            </button>
            <button
              className={`rounded-[14px] px-4 py-2 text-sm font-semibold ${tab === 'products' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600'}`}
              onClick={() => changeTab('products')}
              type="button"
            >
              Productos
            </button>
          </div>
          <StatusBadge label={`${currentQuery.data?.totalItems ?? 0} registro(s)`} tone="info" />
        </div>

        <FilterBar
          actions={
            <>
              <Button onClick={applyFilters} size="sm" type="button">Aplicar filtros</Button>
              <Button onClick={clearFilters} size="sm" type="button" variant="secondary">Limpiar</Button>
            </>
          }
        >
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Búsqueda</span>
            <input
              className={fieldClassName}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Buscar por nombre, categoria o descripcion"
              type="search"
              value={searchInput}
            />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Categoria</span>
            <select className={fieldClassName} onChange={(event) => setCategoryInput(event.target.value)} value={categoryInput}>
              <option value="">Todas</option>
              {categories.map((category) => (
                <option key={category.id} value={category.code}>{category.name}</option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Estado</span>
            <select className={fieldClassName} onChange={(event) => setActiveInput(event.target.value as ActiveFilter)} value={activeInput}>
              <option value="">Todos</option>
              <option value="true">Activos</option>
              <option value="false">Desactivados</option>
            </select>
          </label>
        </FilterBar>

        {inlineError ? (
          <div className="rounded-[18px] border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-800">
            {inlineError}
          </div>
        ) : null}

        {currentQuery.isPending && !currentQuery.data ? (
          <LoadingState message="Cargando catálogo real desde el servidor." variant="table" />
        ) : null}

        {currentQuery.isError && !currentQuery.data ? (
          <ErrorState
            description="No fue posible recuperar el catálogo. Verifica que el servidor este levantado y que las migraciones se hayan ejecutado."
            onRetry={() => void currentQuery.refetch()}
            title="No fue posible cargar el catálogo"
          />
        ) : null}

        {currentQuery.data && currentItems.length === 0 ? (
          <EmptyState
            description="No hay elementos que coincidan con los filtros aplicados. Puedes crear uno nuevo desde las acciones superiores."
            primaryAction={{ label: tab === 'services' ? 'Crear servicio' : 'Crear producto', to: tab === 'services' ? '/catalog/services/new' : '/catalog/products/new' }}
            title="Catálogo sin resultados"
          />
        ) : null}

        {currentQuery.data ? (
          <div className="grid gap-3" data-testid="catalog-list">
            <div className="grid gap-2 rounded-[18px] border border-[var(--color-border)] bg-slate-50 px-4 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 md:grid-cols-[minmax(0,1fr)_minmax(0,0.8fr)_minmax(0,1fr)_110px_110px_auto] md:items-center">
              <span>Nombre</span>
              <span>Categoría</span>
              <span>Detalle</span>
              <span>Estado</span>
              <span>Precio</span>
              <span className="md:text-right">Acciones</span>
            </div>
            {currentItems.length === 0 ? (
              <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
                No hay registros para los filtros seleccionados.
              </div>
            ) : (
              currentItems.map((item) => {
                const active = isRegistroActivo(item.status)

                return (
                  <article
                    className={[
                      'grid gap-3 rounded-[20px] border p-4 transition md:grid-cols-[minmax(0,1fr)_minmax(0,0.8fr)_minmax(0,1fr)_110px_110px_auto] md:items-center',
                      active ? 'border-[var(--color-border)] bg-white' : 'border-amber-200 bg-amber-50/70',
                    ].join(' ')}
                    key={item.id}
                  >
                    <div className="min-w-0">
                      <p className="truncate font-semibold text-[var(--color-text)]">{item.name}</p>
                      <p className="mt-1 truncate text-xs uppercase tracking-[0.16em] text-slate-500">{item.code}</p>
                    </div>
                    <p className="truncate text-sm text-slate-700">{item.category}</p>
                    <p className="text-sm text-slate-700">{item.detail}</p>
                    <StatusBadge label={formatEstadoRegistro(item.status)} tone={getRegistroTone(item.status)} />
                    <p className="text-sm font-semibold text-slate-950">{formatMoney(item.price)}</p>
                    <div className="flex flex-wrap justify-end gap-2">
                      {active ? (
                        <Link className={buttonClassName({ size: 'sm', variant: 'secondary' })} title={`Editar ${item.name}`} to={item.href}>Editar</Link>
                      ) : (
                        <span aria-disabled="true" className={`${buttonClassName({ size: 'sm', variant: 'secondary' })} pointer-events-none opacity-60`} title="No se puede editar un registro desactivado.">Editar</span>
                      )}
                      <Button
                        disabled={statusMutation.isPending}
                        onClick={() => setRowToToggle({ active: !active, row: item })}
                        size="sm"
                        title={active ? `Desactivar ${item.name}` : `Activar ${item.name}`}
                        variant={active ? 'danger' : 'secondary'}
                      >
                        {active ? 'Desactivar' : 'Activar'}
                      </Button>
                    </div>
                  </article>
                )
              })
            )}
          </div>
        ) : null}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
          <p className="text-sm text-slate-600">
            Página {(currentQuery.data?.page ?? 0) + 1} de {Math.max(currentQuery.data?.totalPages ?? 1, 1)} · 10 registros por página
          </p>
          <div className="flex gap-2">
            <Button disabled={page === 0 || currentQuery.isFetching} onClick={() => setPage((current) => Math.max(current - 1, 0))} size="sm" variant="secondary">Anterior</Button>
            <Button disabled={!currentQuery.data || currentQuery.data.totalPages === 0 || page >= currentQuery.data.totalPages - 1 || currentQuery.isFetching} onClick={() => setPage((current) => current + 1)} size="sm" variant="secondary">Siguiente</Button>
          </div>
        </div>
      </Card>

      <ConfirmDialog
        confirmLabel={rowToToggle?.active ? 'Activar' : 'Desactivar'}
        confirmLoading={statusMutation.isPending}
        description={rowToToggle ? `El ${rowToToggle.row.type.toLowerCase()} ${rowToToggle.row.name} quedara ${rowToToggle.active ? 'activo' : 'desactivado'}, pero no será eliminado físicamente.` : 'Confirma el cambio de estado del item.'}
        onCancel={() => setRowToToggle(null)}
        onConfirm={() => {
          if (rowToToggle) {
            statusMutation.mutate(rowToToggle)
          }
        }}
        open={Boolean(rowToToggle)}
        title={rowToToggle?.active ? 'Activar item del catálogo' : 'Desactivar item del catálogo'}
        tone={rowToToggle?.active ? 'neutral' : 'danger'}
      />
    </section>
  )
}

function MetricCard({ label, tone, value }: { label: string; tone: 'success' | 'warning' | 'info'; value: string }) {
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--color-text-secondary)]">{label}</p>
          <p className="mt-2 text-2xl font-semibold tracking-[-0.03em] text-[var(--color-text)]">{value}</p>
        </div>
        <StatusBadge label="BD" tone={tone} />
      </div>
    </Card>
  )
}
