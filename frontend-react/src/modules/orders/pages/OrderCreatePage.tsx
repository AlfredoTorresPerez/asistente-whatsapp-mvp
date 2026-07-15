import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { listCatalogProducts } from '../../../services/api/catalogEtapa8Api'
import {
  createOrder,
  createOrderFromConversation,
  createOrderFromProspect,
} from '../../../services/api/ordersApi'
import type { CreateOrderItemRequest, CreateOrderRequest } from '../../../services/api/types'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const formatMoney = (value: number) =>
  new Intl.NumberFormat('es-CL', {
    currency: 'CLP',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)

export function OrderCreatePage() {
  const { conversationId, prospectId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [customerEmail, setCustomerEmail] = useState('')
  const [status, setStatus] = useState('DRAFT')
  const [discountAmount, setDiscountAmount] = useState('0')
  const [dueDate, setDueDate] = useState('')
  const [notes, setNotes] = useState('')
  const [selectedProductId, setSelectedProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [items, setItems] = useState<CreateOrderItemRequest[]>([])
  const [formError, setFormError] = useState<string | null>(null)

  const productsQuery = useQuery({
    queryKey: ['catalog-products-for-orders'],
    queryFn: () => listCatalogProducts({ active: true, size: 100 }),
  })
  const products = useMemo(() => productsQuery.data?.items ?? [], [productsQuery.data?.items])

  const total = useMemo(() => {
    const subtotal = items.reduce((sum, item) => {
      const product = products.find((current) => current.id === item.productId)
      return sum + (product?.price ?? 0) * item.quantity
    }, 0)
    return Math.max(subtotal - Number(discountAmount || 0), 0)
  }, [discountAmount, items, products])

  const addItem = () => {
    if (!selectedProductId) {
      setFormError('Selecciona un producto para agregarlo al pedido.')
      return
    }
    const amount = Math.max(Number(quantity || 1), 1)
    setItems((current) => {
      const existing = current.find((item) => item.productId === selectedProductId)
      if (existing) {
        return current.map((item) =>
          item.productId === selectedProductId
            ? { ...item, quantity: item.quantity + amount }
            : item,
        )
      }
      return [...current, { productId: selectedProductId, quantity: amount }]
    })
    setSelectedProductId('')
    setQuantity('1')
    setFormError(null)
  }

  const mutation = useMutation({
    mutationFn: async () => {
      setFormError(null)
      if (!conversationId && !prospectId && (!customerName.trim() || !customerPhone.trim())) {
        throw new Error('Debes informar nombre y telefono del cliente.')
      }
      const request: CreateOrderRequest = {
        customerName: customerName.trim() || null,
        customerPhone: customerPhone.trim() || null,
        customerEmail: customerEmail.trim() || null,
        status,
        discountAmount: Number(discountAmount || 0),
        dueDate: dueDate || null,
        notes: notes.trim() || null,
        items,
      }
      if (conversationId) {
        return createOrderFromConversation(conversationId, request)
      }
      if (prospectId) {
        return createOrderFromProspect(prospectId, request)
      }
      return createOrder(request)
    },
    onError: (error) =>
      setFormError(error instanceof Error ? error.message : 'No fue posible crear el pedido.'),
    onSuccess: async (order) => {
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
      navigate(`/orders/${order.id}`)
    },
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link className={buttonClassName({ variant: 'secondary' })} to="/orders">
            Volver a pedidos
          </Link>
        }
        description="Crea pedidos manuales o contextuales desde conversacion o prospecto, agregando productos y calculando total."
        eyebrow="Pedidos"
        title={
          conversationId
            ? 'Crear pedido desde conversacion'
            : prospectId
              ? 'Crear pedido desde prospecto'
              : 'Crear pedido'
        }
      />

      {formError ? (
        <ErrorState title="No fue posible guardar el pedido" description={formError} />
      ) : null}

      <form
        className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]"
        onSubmit={(event) => {
          event.preventDefault()
          mutation.mutate()
        }}
      >
        <Card className="space-y-5">
          {!conversationId && !prospectId ? (
            <div className="grid gap-4 md:grid-cols-3">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">Cliente</span>
                <input
                  className={fieldClassName}
                  onChange={(event) => setCustomerName(event.target.value)}
                  value={customerName}
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">Telefono</span>
                <input
                  className={fieldClassName}
                  onChange={(event) => setCustomerPhone(event.target.value)}
                  value={customerPhone}
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">Correo</span>
                <input
                  className={fieldClassName}
                  onChange={(event) => setCustomerEmail(event.target.value)}
                  value={customerEmail}
                />
              </label>
            </div>
          ) : null}

          <div className="grid gap-4 md:grid-cols-3">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Estado</span>
              <select
                className={fieldClassName}
                onChange={(event) => setStatus(event.target.value)}
                value={status}
              >
                <option value="DRAFT">Pendiente</option>
                <option value="CONFIRMED">Confirmado</option>
                <option value="PREPARING">En preparación</option>
                <option value="READY">Listo</option>
                <option value="DELIVERED">Entregado</option>
                <option value="CANCELLED">Cancelado</option>
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Descuento</span>
              <input
                className={fieldClassName}
                min="0"
                onChange={(event) => setDiscountAmount(event.target.value)}
                type="number"
                value={discountAmount}
              />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Vencimiento</span>
              <input
                className={fieldClassName}
                onChange={(event) => setDueDate(event.target.value)}
                type="date"
                value={dueDate}
              />
            </label>
          </div>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Notas</span>
            <textarea
              className={fieldClassName}
              onChange={(event) => setNotes(event.target.value)}
              rows={4}
              value={notes}
            />
          </label>

          <div className="rounded-[24px] border border-[var(--color-border)] p-4">
            <h2 className="text-lg font-semibold text-[var(--color-text)]">Agregar productos</h2>
            <div className="mt-4 grid gap-4 md:grid-cols-[1fr_140px_auto]">
              <select
                className={fieldClassName}
                onChange={(event) => setSelectedProductId(event.target.value)}
                value={selectedProductId}
              >
                <option value="">Selecciona producto</option>
                {products.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name} · stock {product.stock} · {formatMoney(product.price)}
                  </option>
                ))}
              </select>
              <input
                className={fieldClassName}
                min="1"
                onChange={(event) => setQuantity(event.target.value)}
                type="number"
                value={quantity}
              />
              <Button onClick={addItem}>Agregar</Button>
            </div>
          </div>
        </Card>

        <Card className="space-y-5">
          <h2 className="text-lg font-semibold text-[var(--color-text)]">
            Vista previa simple de comprobante
          </h2>
          <div className="space-y-3">
            {items.length === 0 ? (
              <p className="text-sm text-slate-500">Sin productos agregados.</p>
            ) : (
              items.map((item) => {
                const product = products.find((current) => current.id === item.productId)
                return (
                  <div className="flex justify-between gap-4 text-sm" key={item.productId}>
                    <span>
                      {product?.name ?? item.productId} x{item.quantity}
                    </span>
                    <strong>{formatMoney((product?.price ?? 0) * item.quantity)}</strong>
                  </div>
                )
              })
            )}
          </div>
          <div className="border-t border-[var(--color-border)] pt-4">
            <div className="flex justify-between text-sm">
              <span>Descuento</span>
              <strong>{formatMoney(Number(discountAmount || 0))}</strong>
            </div>
            <div className="mt-2 flex justify-between text-xl font-semibold">
              <span>Total</span>
              <strong>{formatMoney(total)}</strong>
            </div>
          </div>
          <Button fullWidth loading={mutation.isPending} type="submit">
            Guardar pedido
          </Button>
        </Card>
      </form>
    </section>
  )
}

export const ConversationOrderCreatePage = OrderCreatePage
export const ProspectOrderCreatePage = OrderCreatePage
