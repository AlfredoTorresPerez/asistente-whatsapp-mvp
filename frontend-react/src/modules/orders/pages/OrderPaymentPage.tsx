import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { registerOrderPayment } from '../../../services/api/ordersApi'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

export function OrderPaymentPage() {
  const { orderId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState('TRANSFER')
  const [paidAt, setPaidAt] = useState('')
  const [reference, setReference] = useState('')
  const [notes, setNotes] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => registerOrderPayment(orderId ?? '', {
      amount: Number(amount || 0),
      method,
      paidAt: paidAt ? new Date(paidAt).toISOString() : null,
      reference: reference.trim() || null,
      notes: notes.trim() || null,
    }),
    onError: (error) => setFormError(error instanceof Error ? error.message : 'No fue posible registrar el pago.'),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
      navigate(`/orders/${orderId}`)
    },
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={<Link className={buttonClassName({ variant: 'secondary' })} to={`/orders/${orderId}`}>Volver al pedido</Link>}
        description="Registra pagos parciales o totales y recalcula el saldo del pedido."
        eyebrow="Pedidos"
        title="Registrar pago"
      />
      {formError ? <ErrorState title="No fue posible registrar el pago" description={formError} /> : null}
      <Card>
        <form className="grid gap-5 md:grid-cols-2" onSubmit={(event) => { event.preventDefault(); mutation.mutate() }}>
          <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">Monto pagado</span><input className={fieldClassName} min="1" onChange={(event) => setAmount(event.target.value)} required type="number" value={amount} /></label>
          <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">Medio de pago</span><select className={fieldClassName} onChange={(event) => setMethod(event.target.value)} value={method}><option value="TRANSFER">Transferencia</option><option value="CASH">Efectivo</option><option value="CARD">Tarjeta</option><option value="WEBPAY">WebPay</option></select></label>
          <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">Fecha de pago</span><input className={fieldClassName} onChange={(event) => setPaidAt(event.target.value)} type="datetime-local" value={paidAt} /></label>
          <label className="block"><span className="mb-2 block text-sm font-medium text-slate-700">Referencia</span><input className={fieldClassName} onChange={(event) => setReference(event.target.value)} value={reference} /></label>
          <label className="block md:col-span-2"><span className="mb-2 block text-sm font-medium text-slate-700">Notas</span><textarea className={fieldClassName} onChange={(event) => setNotes(event.target.value)} rows={4} value={notes} /></label>
          <div className="md:col-span-2"><Button loading={mutation.isPending} type="submit">Confirmar pago</Button></div>
        </form>
      </Card>
    </section>
  )
}
