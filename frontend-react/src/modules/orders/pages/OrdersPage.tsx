import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { DataTableShell } from '../../../components/ui/DataTableShell'
import { FilterBar } from '../../../components/ui/FilterBar'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { formatEstadoPago, formatEstadoPedido, getEstadoTone } from '../../../lib/statusFormatters'
import { listOrders } from '../../../services/api/ordersApi'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const PAGE_SIZE = 10

const formatMoney = (value: number, currency = 'CLP') =>
  new Intl.NumberFormat('es-CL', { currency, maximumFractionDigits: 0, style: 'currency' }).format(
    value,
  )

export function OrdersPage() {
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [statusInput, setStatusInput] = useState('')
  const [paymentStatusInput, setPaymentStatusInput] = useState('')
  const [filters, setFilters] = useState({ search: '', status: '', paymentStatus: '' })

  const ordersQuery = useQuery({
    queryKey: ['orders', page, filters],
    queryFn: () => listOrders({ page, size: PAGE_SIZE, ...filters }),
    placeholderData: keepPreviousData,
  })

  const orders = useMemo(() => ordersQuery.data?.items ?? [], [ordersQuery.data?.items])
  const metrics = useMemo(() => {
    const open = orders.filter((order) => !['DELIVERED', 'CANCELLED'].includes(order.status)).length
    const paid = orders.reduce((sum, order) => sum + order.paidAmount, 0)
    const pending = orders.reduce((sum, order) => sum + order.balanceDue, 0)
    const total = orders.reduce((sum, order) => sum + order.totalAmount, 0)
    return { open, paid, pending, total }
  }, [orders])

  const applyFilters = () => {
    setPage(0)
    setFilters({ search: searchInput, status: statusInput, paymentStatus: paymentStatusInput })
  }

  const clearFilters = () => {
    setPage(0)
    setSearchInput('')
    setStatusInput('')
    setPaymentStatusInput('')
    setFilters({ search: '', status: '', paymentStatus: '' })
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link className={buttonClassName()} to="/orders/new">
            Crear pedido
          </Link>
        }
        description="Gestiona pedidos reales, pagos, estados y resumen por WhatsApp experimental."
        eyebrow="Pedidos"
        title="Pedidos"
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Pedidos abiertos" value={String(metrics.open)} tone="info" />
        <MetricCard label="Total listado" value={formatMoney(metrics.total)} tone="success" />
        <MetricCard label="Cobrado" value={formatMoney(metrics.paid)} tone="success" />
        <MetricCard
          label="Saldo pendiente"
          value={formatMoney(metrics.pending)}
          tone={metrics.pending > 0 ? 'warning' : 'success'}
        />
      </div>

      <Card className="space-y-5">
        <FilterBar
          actions={
            <>
              <Button onClick={applyFilters}>Buscar</Button>
              <Button onClick={clearFilters} variant="secondary">
                Limpiar
              </Button>
            </>
          }
        >
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Busqueda</span>
            <input
              className={fieldClassName}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Cliente, telefono o estado"
              type="search"
              value={searchInput}
            />
          </label>
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Estado pedido</span>
            <select
              className={fieldClassName}
              onChange={(event) => setStatusInput(event.target.value)}
              value={statusInput}
            >
              <option value="">Todos</option>
              <option value="DRAFT">Borrador</option>
              <option value="CONFIRMED">Confirmado</option>
              <option value="PREPARING">En preparación</option>
              <option value="READY">Listo</option>
              <option value="DELIVERED">Entregado</option>
              <option value="CANCELLED">Cancelado</option>
            </select>
          </label>
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Estado pago</span>
            <select
              className={fieldClassName}
              onChange={(event) => setPaymentStatusInput(event.target.value)}
              value={paymentStatusInput}
            >
              <option value="">Todos</option>
              <option value="PENDING">Pendiente de pago</option>
              <option value="PARTIAL">Pago parcial</option>
              <option value="PAID">Pagado</option>
            </select>
          </label>
        </FilterBar>

        {ordersQuery.isPending && !ordersQuery.data ? (
          <LoadingState message="Cargando pedidos reales." variant="table" />
        ) : null}
        {ordersQuery.isError ? (
          <ErrorState
            title="No fue posible cargar los pedidos"
            description="Revisa la conexion con el backend y vuelve a intentar."
          />
        ) : null}
        {!ordersQuery.isPending && !ordersQuery.isError && orders.length === 0 ? (
          <EmptyState
            title="Sin pedidos"
            description="Crea un pedido para comenzar a operar el flujo comercial."
          />
        ) : null}
        {ordersQuery.data && !ordersQuery.isError ? (
          <DataTableShell
            caption="Listado de pedidos"
            columns={['Pedido', 'Cliente', 'Estado', 'Pago', 'Total']}
            emptyMessage="No hay pedidos para los filtros seleccionados."
            rows={orders.map((order) => ({
              id: order.id,
              href: `/orders/${order.id}`,
              cells: [
                order.orderNumber,
                `${order.customerName} · ${order.customerPhone}`,
                <StatusBadge
                  key="status"
                  label={formatEstadoPedido(order.status)}
                  tone={getEstadoTone(order.status)}
                />,
                <StatusBadge
                  key="payment"
                  label={formatEstadoPago(order.paymentStatus)}
                  tone={getEstadoTone(order.paymentStatus)}
                />,
                formatMoney(order.totalAmount, order.currency),
              ],
            }))}
          />
        ) : null}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-5">
          <p className="text-sm text-slate-600">
            Pagina {(ordersQuery.data?.page ?? 0) + 1} de{' '}
            {Math.max(ordersQuery.data?.totalPages ?? 1, 1)} · 10 registros por pagina
          </p>
          <div className="flex gap-2">
            <Button
              disabled={page === 0 || ordersQuery.isFetching}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              variant="secondary"
            >
              Anterior
            </Button>
            <Button
              disabled={
                !ordersQuery.data ||
                page + 1 >= ordersQuery.data.totalPages ||
                ordersQuery.isFetching
              }
              onClick={() => setPage((current) => current + 1)}
              variant="secondary"
            >
              Siguiente
            </Button>
          </div>
        </div>
      </Card>
    </section>
  )
}

function MetricCard({
  label,
  value,
  tone,
}: {
  label: string
  value: string
  tone: 'success' | 'warning' | 'info'
}) {
  return (
    <Card className="space-y-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">{label}</p>
        <StatusBadge label={tone === 'warning' ? 'Revisar' : 'OK'} tone={tone} />
      </div>
      <p className="text-3xl font-semibold text-[var(--color-text)]">{value}</p>
    </Card>
  )
}
