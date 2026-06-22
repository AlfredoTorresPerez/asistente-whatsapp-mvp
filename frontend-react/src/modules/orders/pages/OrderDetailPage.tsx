import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { DataTableShell } from '../../../components/ui/DataTableShell'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { formatEstadoPago, formatEstadoPedido, getEstadoTone } from '../../../lib/statusFormatters'
import { getOrder, sendOrderSummary, updateOrderStatus } from '../../../services/api/ordersApi'

const formatMoney = (value: number, currency = 'CLP') =>
  new Intl.NumberFormat('es-CL', { currency, maximumFractionDigits: 0, style: 'currency' }).format(value)

const orderStatusActions = ['CONFIRMED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED']

export function OrderDetailPage() {
  const { orderId } = useParams()
  const queryClient = useQueryClient()
  const orderQuery = useQuery({
    queryKey: ['orders', orderId],
    queryFn: () => getOrder(orderId ?? ''),
    enabled: Boolean(orderId),
  })
  const statusMutation = useMutation({
    mutationFn: (status: string) => updateOrderStatus(orderId ?? '', status),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
  const sendMutation = useMutation({
    mutationFn: () => sendOrderSummary(orderId ?? ''),
  })

  if (orderQuery.isPending) return <LoadingState message="Cargando detalle del pedido." variant="detail" />
  if (orderQuery.isError || !orderQuery.data) return <ErrorState title="No fue posible cargar el pedido" description="Verifica que el pedido exista y que el backend este disponible." />

  const order = orderQuery.data
  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Link className={buttonClassName()} to={`/orders/${order.id}/payments/new`}>Registrar pago</Link>
            <Link className={buttonClassName({ variant: 'secondary' })} to="/orders">Volver a pedidos</Link>
          </>
        }
        description="Detalle de pedido, productos, pagos, saldo y acciones operativas."
        eyebrow="Pedidos"
        title={`Detalle de pedido ${order.orderNumber}`}
      />

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card className="space-y-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-sm text-[var(--color-text-secondary)]">Cliente</p>
              <h2 className="mt-1 text-2xl font-semibold text-[var(--color-text)]">{order.customerName}</h2>
              <p className="mt-1 text-sm text-slate-500">{order.customerPhone}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <StatusBadge label={formatEstadoPedido(order.status)} tone={getEstadoTone(order.status)} />
              <StatusBadge label={formatEstadoPago(order.paymentStatus)} tone={getEstadoTone(order.paymentStatus)} />
            </div>
          </div>

          <DataTableShell
            caption="Productos del pedido"
            columns={['Producto', 'Cantidad', 'Precio unitario', 'Total']}
            rows={order.items.map((item) => ({
              id: item.id,
              cells: [item.productName, item.quantity, formatMoney(item.unitPrice, order.currency), formatMoney(item.lineTotal, order.currency)],
            }))}
          />

          <DataTableShell
            caption="Pagos registrados"
            columns={['Metodo', 'Monto', 'Fecha', 'Referencia']}
            rows={order.payments.map((payment) => ({
              id: payment.id,
              cells: [payment.method, formatMoney(payment.amount, order.currency), new Date(payment.paidAt).toLocaleString('es-CL'), payment.reference ?? '-'],
            }))}
            emptyMessage="No hay pagos registrados."
          />
        </Card>

        <Card className="space-y-5">
          <h2 className="text-lg font-semibold text-[var(--color-text)]">Vista previa simple de comprobante</h2>
          <pre className="whitespace-pre-wrap rounded-[18px] bg-slate-50 p-4 text-sm leading-6 text-slate-700">{order.receiptPreview}</pre>
          <div className="grid gap-3 text-sm">
            <Line label="Subtotal" value={formatMoney(order.subtotalAmount, order.currency)} />
            <Line label="Descuento" value={formatMoney(order.discountAmount, order.currency)} />
            <Line label="Total" value={formatMoney(order.totalAmount, order.currency)} />
            <Line label="Pagado" value={formatMoney(order.paidAmount, order.currency)} />
            <Line label="Saldo" value={formatMoney(order.balanceDue, order.currency)} />
          </div>
          <Button fullWidth loading={sendMutation.isPending} onClick={() => sendMutation.mutate()} variant="secondary">Enviar resumen por WhatsApp</Button>
          {sendMutation.isSuccess ? <p className="text-sm font-semibold text-emerald-700">Resumen enviado o encolado correctamente.</p> : null}
          {sendMutation.isError ? <p className="text-sm font-semibold text-red-700">No fue posible enviar el resumen.</p> : null}
          <div className="grid gap-2">
            {orderStatusActions.map((status) => (
              <Button disabled={order.status === status} key={status} loading={statusMutation.isPending} onClick={() => statusMutation.mutate(status)} variant="secondary">Cambiar a {formatEstadoPedido(status)}</Button>
            ))}
          </div>
        </Card>
      </div>
    </section>
  )
}

function Line({ label, value }: { label: string; value: string }) {
  return <div className="flex justify-between gap-4"><span className="text-slate-500">{label}</span><strong>{value}</strong></div>
}
