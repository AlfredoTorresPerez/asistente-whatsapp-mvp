import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { DataTableShell, type DataTableShellRow } from '../../../components/ui/DataTableShell'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'

type BadgeTone = 'success' | 'warning' | 'danger' | 'neutral' | 'info'

type Metric = {
  label: string
  value: string
  delta: string
  tone: BadgeTone
}

type ActionLink = {
  label: string
  to: string
  variant?: 'primary' | 'secondary' | 'ghost'
}

type ModuleLayoutProps = {
  eyebrow: string
  title: string
  description: string
  actions?: ActionLink[]
  metrics?: Metric[]
  children: ReactNode
  aside?: ReactNode
}

function ModuleLayout({
  actions = [],
  aside,
  children,
  description,
  eyebrow,
  metrics = [],
  title,
}: ModuleLayoutProps) {
  return (
    <section className="space-y-6">
      <PageHeader
        actions={actions.map((action) => (
          <Link
            className={buttonClassName({ variant: action.variant ?? 'primary' })}
            key={`${action.to}-${action.label}`}
            to={action.to}
          >
            {action.label}
          </Link>
        ))}
        description={description}
        eyebrow={eyebrow}
        title={title}
      />

      {metrics.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {metrics.map((metric) => (
            <Card className="p-5" key={metric.label}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-[var(--color-text-secondary)]">
                    {metric.label}
                  </p>
                  <p className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[var(--color-text)]">
                    {metric.value}
                  </p>
                </div>
                <StatusBadge label={metric.delta} tone={metric.tone} />
              </div>
            </Card>
          ))}
        </div>
      ) : null}

      <div className={aside ? 'grid gap-6 2xl:grid-cols-[minmax(0,1fr)_360px]' : 'space-y-6'}>
        <div className="min-w-0 space-y-6">{children}</div>
        {aside ? <aside className="min-w-0 space-y-6">{aside}</aside> : null}
      </div>
    </section>
  )
}

type VisualTableProps = {
  caption: string
  columns: string[]
  rows: DataTableShellRow[]
}

function VisualTable({ caption, columns, rows }: VisualTableProps) {
  return (
    <Card className="p-0">
      <div className="flex flex-col gap-3 border-b border-[var(--color-border)] px-5 py-4 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--color-text-secondary)]">
            Vista operacional
          </p>
          <h2 className="mt-2 text-xl font-semibold text-[var(--color-text)]">{caption}</h2>
        </div>
        <div className="flex flex-wrap gap-2">
          <SearchPill label="Buscar" />
          <SearchPill label="Estado" />
          <SearchPill label="Responsable" />
        </div>
      </div>
      <DataTableShell caption="Listado alineado al contrato visual con filtros, estados y accion contextual por fila." columns={columns} rows={rows} />
    </Card>
  )
}

function SearchPill({ label }: { label: string }) {
  return (
    <span className="inline-flex h-10 items-center rounded-[14px] border border-[var(--color-border)] bg-white px-3 text-sm font-medium text-[var(--color-text-secondary)] shadow-[0_8px_18px_rgba(15,23,42,0.03)]">
      {label}
    </span>
  )
}

function PanelCard({ children, title }: { children: ReactNode; title: string }) {
  return (
    <Card className="space-y-4">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--color-text-secondary)]">
          Panel contextual
        </p>
        <h3 className="mt-2 text-lg font-semibold text-[var(--color-text)]">{title}</h3>
      </div>
      {children}
    </Card>
  )
}

function FieldGrid({ fields }: { fields: string[] }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      {fields.map((field) => (
        <label className="block" key={field}>
          <span className="mb-2 block text-sm font-semibold text-[var(--color-text)]">{field}</span>
          <span className="flex h-11 items-center rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm text-[var(--color-text-secondary)]">
            Completar informacion
          </span>
        </label>
      ))}
    </div>
  )
}

function FormShell({
  cancelTo,
  fields,
  saveLabel,
  title,
}: {
  cancelTo: string
  fields: string[]
  saveLabel: string
  title: string
}) {
  return (
    <Card className="space-y-6">
      <div className="flex flex-col gap-3 border-b border-[var(--color-border)] pb-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--color-text-secondary)]">
            Formulario guiado
          </p>
          <h2 className="mt-2 text-xl font-semibold text-[var(--color-text)]">{title}</h2>
        </div>
        <StatusBadge label="Borrador" tone="warning" />
      </div>

      <FieldGrid fields={fields} />

      <div className="rounded-[20px] border border-blue-100 bg-blue-50/70 p-4 text-sm leading-6 text-blue-900">
        Los campos estan organizados como panel blanco, con bordes suaves, jerarquia de etiquetas y acciones al pie para mantener la misma lectura visual del contrato.
      </div>

      <div className="flex flex-wrap justify-end gap-3 border-t border-[var(--color-border)] pt-5">
        <Link className={buttonClassName({ variant: 'secondary' })} to={cancelTo}>
          Cancelar
        </Link>
        <Button>{saveLabel}</Button>
      </div>
    </Card>
  )
}

function Timeline({ items }: { items: Array<{ title: string; detail: string; tone: BadgeTone }> }) {
  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-4" key={item.title}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-[var(--color-text)]">{item.title}</p>
              <p className="mt-1 text-sm leading-6 text-[var(--color-text-secondary)]">{item.detail}</p>
            </div>
            <StatusBadge label="Hoy" tone={item.tone} />
          </div>
        </div>
      ))}
    </div>
  )
}

function Money({ value }: { value: string }) {
  return <span className="font-semibold text-[var(--color-text)]">{value}</span>
}

export function ConversationOrderCreatePage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Volver a conversacion', to: '/conversations/demo-conversation', variant: 'secondary' }]}
      description="Alta de pedido desde el hilo activo, conservando cliente, productos, totales y notas comerciales en un panel claro."
      eyebrow="Conversaciones"
      title="Crear pedido desde conversacion"
    >
      <FormShell
        cancelTo="/conversations/demo-conversation"
        fields={[
          'Cliente',
          'Servicio o producto',
          'Cantidad',
          'Precio unitario',
          'Descuento',
          'Notas del pedido',
        ]}
        saveLabel="Crear pedido"
        title="Informacion del pedido"
      />
    </ModuleLayout>
  )
}

const orderRows: DataTableShellRow[] = [
  {
    id: 'order-1',
    cells: ['#PED-00006 · Sofia Rojas', <StatusBadge key="s1" label="Pago parcial" tone="warning" />, <Money key="m1" value="$54.980" />],
    href: '/orders/demo-order',
  },
  {
    id: 'order-2',
    cells: ['#PED-00007 · Carlos Mendez', <StatusBadge key="s2" label="Pagado" tone="success" />, <Money key="m2" value="$89.990" />],
    href: '/orders/demo-order',
  },
  {
    id: 'order-3',
    cells: ['#PED-00008 · Ana Torres', <StatusBadge key="s3" label="Pendiente" tone="info" />, <Money key="m3" value="$29.990" />],
    href: '/orders/demo-order',
  },
]

export function OrdersPage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Crear pedido', to: '/orders/new' }, { label: 'Exportar', to: '/reports', variant: 'secondary' }]}
      aside={
        <PanelCard title="Resumen de cobros">
          <Timeline
            items={[
              { title: 'Pago recibido', detail: 'Transferencia confirmada para #PED-00007.', tone: 'success' },
              { title: 'Saldo pendiente', detail: 'Sofia Rojas mantiene saldo por $18.000.', tone: 'warning' },
              { title: 'Despacho coordinado', detail: 'Pedido preparado para retiro en sucursal.', tone: 'info' },
            ]}
          />
        </PanelCard>
      }
      description="Gestiona pedidos, pagos, despachos y comprobantes desde una vista visualmente alineada al contrato."
      eyebrow="Pedidos"
      metrics={[
        { label: 'Pedidos abiertos', value: '24', delta: '+8%', tone: 'success' },
        { label: 'Cobrado', value: '$1.8M', delta: 'Mes', tone: 'info' },
        { label: 'Pendiente', value: '$320K', delta: 'Alerta', tone: 'warning' },
        { label: 'Ticket medio', value: '$74K', delta: '+5%', tone: 'success' },
      ]}
      title="Pedidos"
    >
      <VisualTable caption="Listado de pedidos" columns={['Pedido', 'Cobro', 'Total']} rows={orderRows} />
    </ModuleLayout>
  )
}

export function OrderCreatePage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Volver a pedidos', to: '/orders', variant: 'secondary' }]}
      description="Registra productos, totales y vencimiento con estructura de formulario del contrato visual."
      eyebrow="Pedidos"
      title="Crear pedido"
    >
      <FormShell
        cancelTo="/orders"
        fields={['Cliente', 'Producto o servicio', 'Cantidad', 'Precio', 'Medio de pago', 'Fecha de vencimiento']}
        saveLabel="Guardar pedido"
        title="Informacion comercial"
      />
    </ModuleLayout>
  )
}

export function OrderDetailPage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Registrar pago', to: '/orders/demo-order/payments/new' }, { label: 'Volver a pedidos', to: '/orders', variant: 'secondary' }]}
      aside={
        <PanelCard title="Acciones rapidas">
          <div className="grid gap-3">
            <Link className={buttonClassName({ variant: 'secondary', fullWidth: true })} to="/orders/demo-order/payments/new">
              Registrar pago
            </Link>
            <Button fullWidth variant="secondary">Enviar resumen WhatsApp</Button>
            <Button fullWidth variant="secondary">Emitir comprobante</Button>
          </div>
        </PanelCard>
      }
      description="Detalle de pedido con items, pagos, saldo pendiente y acciones operativas."
      eyebrow="Pedidos"
      title="Detalle de pedido #PED-00006"
    >
      <Card className="space-y-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm text-[var(--color-text-secondary)]">Cliente</p>
            <h2 className="mt-1 text-2xl font-semibold text-[var(--color-text)]">Sofia Rojas</h2>
          </div>
          <StatusBadge label="Pago parcial" tone="warning" />
        </div>
        <VisualTable caption="Items del pedido" columns={['Item', 'Cantidad', 'Total']} rows={orderRows.slice(0, 2)} />
      </Card>
    </ModuleLayout>
  )
}

export function OrderPaymentPage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Volver al pedido', to: '/orders/demo-order', variant: 'secondary' }]}
      description="Registro de pago y recalculo de saldo dentro del flujo de pedidos."
      eyebrow="Pedidos"
      title="Registrar pago"
    >
      <FormShell
        cancelTo="/orders/demo-order"
        fields={['Monto pagado', 'Medio de pago', 'Fecha de pago', 'Referencia bancaria']}
        saveLabel="Confirmar pago"
        title="Informacion del pago"
      />
    </ModuleLayout>
  )
}

const catalogRows: DataTableShellRow[] = [
  { id: 'p1', cells: ['Limpieza Facial Profunda', <StatusBadge key="c1" label="Activa" tone="success" />, <Money key="cm1" value="$34.990" />], href: '/catalog/products/demo-product/edit' },
  { id: 'p2', cells: ['Masaje relajante', <StatusBadge key="c2" label="Activa" tone="success" />, <Money key="cm2" value="$29.990" />], href: '/catalog/products/demo-product/edit' },
  { id: 'p3', cells: ['Tratamiento hidratante', <StatusBadge key="c3" label="Pausado" tone="warning" />, <Money key="cm3" value="$44.990" />], href: '/catalog/products/demo-product/edit' },
]

export function CatalogPage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Crear producto', to: '/catalog/products/new' }, { label: 'Importar catalogo', to: '/catalog', variant: 'secondary' }]}
      aside={
        <PanelCard title="Categorias e inventario">
          <Timeline
            items={[
              { title: 'Servicios faciales', detail: '12 productos activos con disponibilidad inmediata.', tone: 'success' },
              { title: 'Corporales', detail: '5 servicios requieren revisar stock de insumos.', tone: 'warning' },
              { title: 'Promociones', detail: '3 combos disponibles para campanas.', tone: 'info' },
            ]}
          />
        </PanelCard>
      }
      description="Catalogo de productos y servicios con categorias, precios, stock y estados comerciales."
      eyebrow="Catalogo"
      metrics={[
        { label: 'Activos', value: '38', delta: '+4', tone: 'success' },
        { label: 'Categorias', value: '7', delta: 'OK', tone: 'info' },
        { label: 'Stock bajo', value: '5', delta: 'Revisar', tone: 'warning' },
        { label: 'Promos', value: '3', delta: 'Online', tone: 'success' },
      ]}
      title="Catalogo"
    >
      <VisualTable caption="Productos y servicios" columns={['Producto o servicio', 'Estado', 'Precio']} rows={catalogRows} />
    </ModuleLayout>
  )
}

export function CatalogCreatePage() {
  return (
    <ModuleLayout actions={[{ label: 'Volver al catalogo', to: '/catalog', variant: 'secondary' }]} description="Alta de producto o servicio con precio, categoria, inventario y visibilidad." eyebrow="Catalogo" title="Crear producto">
      <FormShell cancelTo="/catalog" fields={['Nombre', 'Categoria', 'Precio', 'Duracion', 'Stock disponible', 'Estado']} saveLabel="Guardar producto" title="Informacion del catalogo" />
    </ModuleLayout>
  )
}

export function CatalogEditPage() {
  return (
    <ModuleLayout actions={[{ label: 'Volver al catalogo', to: '/catalog', variant: 'secondary' }]} description="Actualiza precio, categoria, disponibilidad y estado del producto." eyebrow="Catalogo" title="Editar producto">
      <FormShell cancelTo="/catalog" fields={['Nombre', 'SKU', 'Categoria', 'Precio', 'Stock minimo', 'Estado']} saveLabel="Guardar cambios" title="Limpieza Facial Profunda" />
    </ModuleLayout>
  )
}

const ruleRows: DataTableShellRow[] = [
  { id: 'r1', cells: ['Seguimiento palabra precio', <StatusBadge key="r1" label="Activa" tone="success" />, 'KEYWORD_MATCH'], href: '/automation-rules/demo-rule/edit' },
  { id: 'r2', cells: ['Recordatorio de cita', <StatusBadge key="r2" label="Activa" tone="success" />, 'APPOINTMENT_REMINDER'], href: '/automation-rules/demo-rule/edit' },
  { id: 'r3', cells: ['Fuera de horario', <StatusBadge key="r3" label="Pausada" tone="warning" />, 'SCHEDULE_WINDOW'], href: '/automation-rules/demo-rule/edit' },
]

export function AutomationRulesPage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Crear regla', to: '/automation-rules/new' }]}
      aside={
        <PanelCard title="Historial reciente">
          <Timeline
            items={[
              { title: 'Regla activada', detail: 'Respuesta fuera de horario ejecutada correctamente.', tone: 'success' },
              { title: 'Condicion validada', detail: 'Keyword precio detectada en conversacion.', tone: 'info' },
              { title: 'Requiere revision', detail: 'Plantilla sin variable obligatoria.', tone: 'warning' },
            ]}
          />
        </PanelCard>
      }
      description="Automatizaciones comerciales con condiciones, acciones, simulacion e historial operativo."
      eyebrow="Reglas"
      metrics={[
        { label: 'Reglas activas', value: '12', delta: '+2', tone: 'success' },
        { label: 'Ejecuciones', value: '486', delta: '7 dias', tone: 'info' },
        { label: 'Pendientes', value: '3', delta: 'Revisar', tone: 'warning' },
        { label: 'Errores', value: '0', delta: 'OK', tone: 'success' },
      ]}
      title="Reglas de automatizacion"
    >
      <VisualTable caption="Listado de reglas" columns={['Regla', 'Estado', 'Disparador']} rows={ruleRows} />
    </ModuleLayout>
  )
}

export function AutomationRuleCreatePage() {
  return (
    <ModuleLayout actions={[{ label: 'Volver a reglas', to: '/automation-rules', variant: 'secondary' }]} description="Configura nombre, disparador, condiciones y acciones de la regla." eyebrow="Reglas" title="Crear regla">
      <FormShell cancelTo="/automation-rules" fields={['Nombre de la regla', 'Disparador', 'Condicion principal', 'Accion', 'Plantilla', 'Ventana horaria']} saveLabel="Guardar regla" title="Asistente de creacion" />
    </ModuleLayout>
  )
}

export function AutomationRuleEditPage() {
  return (
    <ModuleLayout actions={[{ label: 'Probar regla', to: '/automation-rules/demo-rule/test' }, { label: 'Volver a reglas', to: '/automation-rules', variant: 'secondary' }]} description="Edita condiciones, acciones y plantillas asociadas a la automatizacion." eyebrow="Reglas" title="Editar regla">
      <FormShell cancelTo="/automation-rules" fields={['Nombre', 'Estado', 'Condicion', 'Accion', 'Respuesta', 'Limites']} saveLabel="Guardar cambios" title="Seguimiento palabra precio" />
    </ModuleLayout>
  )
}

export function AutomationRuleRunPage() {
  return (
    <ModuleLayout actions={[{ label: 'Volver a reglas', to: '/automation-rules', variant: 'secondary' }]} description="Simula entrada de cliente y revisa la accion resultante antes de activar cambios." eyebrow="Reglas" title="Probar regla">
      <Card className="space-y-5">
        <FieldGrid fields={['Mensaje de entrada', 'Cliente de prueba', 'Canal', 'Resultado esperado']} />
        <PanelCard title="Resultado de simulacion">
          <StatusBadge label="Coincidencia detectada" tone="success" />
          <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">La condicion se cumple y la plantilla sera enviada segun la ventana horaria configurada.</p>
        </PanelCard>
      </Card>
    </ModuleLayout>
  )
}

const userRows: DataTableShellRow[] = [
  { id: 'u1', cells: ['Ana Admin', <StatusBadge key="u1" label="OWNER" tone="info" />, <StatusBadge key="u1s" label="Activo" tone="success" />], href: '/admin/users/demo-user/edit' },
  { id: 'u2', cells: ['Carla Mendez', <StatusBadge key="u2" label="AGENT" tone="neutral" />, <StatusBadge key="u2s" label="Activo" tone="success" />], href: '/admin/users/demo-user/edit' },
  { id: 'u3', cells: ['Pablo Rivera', <StatusBadge key="u3" label="SUPERVISOR" tone="info" />, <StatusBadge key="u3s" label="Invitado" tone="warning" />], href: '/admin/users/demo-user/edit' },
]

export function AdminUsersPage() {
  return (
    <ModuleLayout actions={[{ label: 'Crear usuario', to: '/admin/users/new' }, { label: 'Volver a administracion', to: '/admin', variant: 'secondary' }]} description="Usuarios, roles, estados de acceso y acciones de administracion." eyebrow="Administracion" title="Usuarios y roles">
      <VisualTable caption="Usuarios del negocio" columns={['Usuario', 'Rol', 'Estado']} rows={userRows} />
    </ModuleLayout>
  )
}

export function AdminUserCreatePage() {
  return (
    <ModuleLayout actions={[{ label: 'Volver a usuarios', to: '/admin/users', variant: 'secondary' }]} description="Alta de usuario con rol y permisos base." eyebrow="Administracion" title="Crear usuario">
      <FormShell cancelTo="/admin/users" fields={['Nombre', 'Correo', 'Telefono', 'Rol', 'Estado', 'Sucursal']} saveLabel="Crear usuario" title="Datos del usuario" />
    </ModuleLayout>
  )
}

export function AdminUserEditPage() {
  return (
    <ModuleLayout actions={[{ label: 'Volver a usuarios', to: '/admin/users', variant: 'secondary' }]} description="Actualiza datos personales, rol y estado de acceso." eyebrow="Administracion" title="Editar usuario">
      <FormShell cancelTo="/admin/users" fields={['Nombre', 'Correo', 'Telefono', 'Rol', 'Estado', 'Permisos']} saveLabel="Guardar cambios" title="Carla Mendez" />
    </ModuleLayout>
  )
}

export function AdminSecurityPage() {
  return (
    <ModuleLayout
      actions={[{ label: 'Volver a administracion', to: '/admin', variant: 'secondary' }]}
      aside={
        <PanelCard title="Sesiones y dispositivos">
          <Timeline
            items={[
              { title: 'Sesion actual', detail: 'Chrome en Windows · Santiago.', tone: 'success' },
              { title: 'Dispositivo movil', detail: 'Android autorizado hace 2 dias.', tone: 'info' },
              { title: 'Acceso antiguo', detail: 'Sesion expirada automaticamente.', tone: 'neutral' },
            ]}
          />
        </PanelCard>
      }
      description="Politicas de contrasena, sesiones, segundo factor, dispositivos y registro de accesos."
      eyebrow="Seguridad"
      metrics={[
        { label: '2FA', value: 'Activo', delta: 'Seguro', tone: 'success' },
        { label: 'Sesiones', value: '2', delta: 'OK', tone: 'info' },
        { label: 'Alertas', value: '0', delta: 'OK', tone: 'success' },
        { label: 'Roles', value: '3', delta: 'Base', tone: 'neutral' },
      ]}
      title="Seguridad"
    >
      <FormShell cancelTo="/admin" fields={['Longitud minima', 'Expiracion de sesion', 'Bloqueo por intentos', 'Segundo factor', 'Dominios permitidos', 'Registro de accesos']} saveLabel="Guardar seguridad" title="Politicas de acceso" />
    </ModuleLayout>
  )
}
