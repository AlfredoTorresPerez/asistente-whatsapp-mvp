import type { ReactNode } from 'react'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { getAdminSummaryRequest } from '../../../services/api/administrationApi'
import type { AdminSummaryResponse } from '../../../services/api/types'
import { AdminContentPage } from './AdminContentPage'

type AdminArea =
  'company' | 'locations' | 'multisite' | 'whatsapp-channel' | 'users' | 'security' | 'content'

function getWhatsAppChannelTone(status: string) {
  switch (status) {
    case 'CONNECTED':
      return 'success'
    case 'ERROR':
      return 'danger'
    default:
      return 'neutral'
  }
}

function toWhatsAppChannelLabel(status: string) {
  switch (status) {
    case 'CONNECTED':
      return 'Conectado'
    case 'ERROR':
      return 'Error'
    case 'DISCONNECTED':
    default:
      return 'Desconectado'
  }
}

export function AdministrationPage() {
  const [activeArea, setActiveArea] = useState<AdminArea>('company')

  const adminSummaryQuery = useQuery({
    queryKey: ['administration', 'summary'],
    queryFn: getAdminSummaryRequest,
  })

  const summary = adminSummaryQuery.data

  return (
    <section className="space-y-4 overflow-hidden">
      <PageHeader
        actions={
          <>
            <Link to="/admin/company">
              <Button>Configuracion de empresa</Button>
            </Link>
            <Link to="/admin/locations">
              <Button variant="secondary">Sedes del negocio</Button>
            </Link>
            <Link to="/admin/multisite">
              <Button variant="secondary">Operacion multisede</Button>
            </Link>
            <Link to="/admin/whatsapp-channel">
              <Button variant="secondary">Canal de WhatsApp</Button>
            </Link>
          </>
        }
        description="Centro de configuracion inicial del negocio, con resumen de empresa, acceso al canal de WhatsApp y atajos a seguridad y usuarios."
        eyebrow="Administración"
        title="Administración"
      />

      {adminSummaryQuery.isPending && !summary ? (
        <LoadingState
          message="Cargando el resumen administrativo del negocio y el estado del canal de WhatsApp."
          variant="page"
        />
      ) : null}

      {adminSummaryQuery.isError && !summary ? (
        <ErrorState
          description="No pudimos recuperar la configuracion general de la empresa. Reintenta para volver a consultar el panel."
          onRetry={() => void adminSummaryQuery.refetch()}
          title="No fue posible cargar administracion"
        />
      ) : null}

      {summary ? (
        <>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <SummaryCard
              label="Empresa activa"
              value={summary.company.companyName}
              note="Razon social actualmente vinculada al negocio."
            />
            <SummaryCard
              label="Usuarios registrados"
              value={String(summary.users.total)}
              note={`${summary.users.active} usuario(s) activo(s) en la operacion actual.`}
            />
            <SummaryCard
              badge={
                <StatusBadge
                  label={toWhatsAppChannelLabel(summary.whatsapp.status)}
                  tone={getWhatsAppChannelTone(summary.whatsapp.status)}
                />
              }
              label="Canal de WhatsApp"
              value="Canal activo"
              note="Conexion segun la configuracion vigente del canal."
            />
            <SummaryCard
              label="Timeout de sesion"
              value={`${summary.security.sessionTimeoutMinutes} min`}
              note={`Snapshot verificado ${dayjs().format('DD/MM/YYYY HH:mm')}.`}
            />
          </div>

          <AdminAreaTabs activeArea={activeArea} onChange={setActiveArea} summary={summary} />

          {activeArea === 'company' ? (
            <CompanyAdminPanel companyName={summary.company.companyName} />
          ) : null}
          {activeArea === 'locations' ? <LocationsAdminPanel /> : null}
          {activeArea === 'multisite' ? <MultisiteAdminPanel /> : null}
          {activeArea === 'whatsapp-channel' ? (
            <WhatsAppAdminPanel status={summary.whatsapp.status} />
          ) : null}
          {activeArea === 'users' ? (
            <UsersAdminPanel activeUsers={summary.users.active} totalUsers={summary.users.total} />
          ) : null}
          {activeArea === 'security' ? (
            <SecurityAdminPanel sessionTimeoutMinutes={summary.security.sessionTimeoutMinutes} />
          ) : null}
          {activeArea === 'content' ? <AdminContentPage /> : null}
        </>
      ) : null}
    </section>
  )
}

function SummaryCard({
  badge,
  label,
  note,
  value,
}: {
  badge?: ReactNode
  label: string
  note: string
  value: string
}) {
  return (
    <Card className="min-h-[128px] p-4">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        {badge ?? <StatusBadge label="Activo" tone="info" />}
      </div>
      <p className="mt-3 text-2xl font-semibold text-slate-950">{value}</p>
      <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">{note}</p>
    </Card>
  )
}

function AdminAreaTabs({
  activeArea,
  onChange,
  summary,
}: {
  activeArea: AdminArea
  onChange: (area: AdminArea) => void
  summary: AdminSummaryResponse
}) {
  const items: {
    badge?: ReactNode
    description: string
    label: string
    value: AdminArea
  }[] = [
    {
      description: 'Datos comerciales, contacto, moneda y zona horaria',
      label: 'Configuracion de empresa',
      value: 'company',
    },
    {
      description: 'Sedes, direccion, telefono y WhatsApp por sucursal',
      label: 'Sedes del negocio',
      value: 'locations',
    },
    {
      description: 'Servicios, horarios, permisos y canales por sede',
      label: 'Operacion multisede',
      value: 'multisite',
    },
    {
      badge: (
        <StatusBadge
          label={toWhatsAppChannelLabel(summary.whatsapp.status)}
          tone={getWhatsAppChannelTone(summary.whatsapp.status)}
        />
      ),
      description: 'Estado, reconexion y pruebas del canal',
      label: 'Canal de WhatsApp',
      value: 'whatsapp-channel',
    },
    {
      badge: <StatusBadge label={`${summary.users.active}/${summary.users.total}`} tone="info" />,
      description: 'Gestion de accesos, perfiles y responsables',
      label: 'Usuarios y roles',
      value: 'users',
    },
    {
      badge: <StatusBadge label={`${summary.security.sessionTimeoutMinutes} min`} tone="neutral" />,
      description: 'Políticas, sesiones y auditoría administrativa',
      label: 'Seguridad',
      value: 'security',
    },
    {
      description: 'Imágenes y textos para categorías, servicios y landing page',
      label: 'Contenido visual',
      value: 'content',
    },
  ]

  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      {items.map((item) => (
        <button
          className={[
            'min-h-[86px] rounded-[20px] border px-4 py-3 text-left transition',
            activeArea === item.value
              ? 'border-blue-200 bg-blue-50 text-blue-900 shadow-sm'
              : 'border-[var(--color-border)] bg-white text-slate-700 hover:border-blue-200',
          ].join(' ')}
          key={item.value}
          onClick={() => onChange(item.value)}
          type="button"
        >
          <span className="flex items-start justify-between gap-3">
            <span className="block text-sm font-semibold">{item.label}</span>
            {item.badge}
          </span>
          <span className="mt-1 block text-xs leading-5 text-slate-500">{item.description}</span>
        </button>
      ))}
    </div>
  )
}

function CompanyAdminPanel({ companyName }: { companyName: string }) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="p-5">
          <StatusBadge label="Empresa" tone="info" />
          <h2 className="mt-3 text-xl font-semibold text-slate-950">Configuracion de empresa</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Administra los datos base de {companyName}: nombre comercial, contacto principal, moneda
            y zona horaria. Esta informacion se reutiliza en paneles, reglas de negocio y respuestas
            asistidas.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link to="/admin/company">
              <Button>Abrir configuracion</Button>
            </Link>
          </div>
        </div>
        <div className="border-t border-[var(--color-border)] bg-slate-50 p-5 lg:border-l lg:border-t-0">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
            Datos principales
          </p>
          <dl className="mt-4 space-y-3 text-sm">
            <div>
              <dt className="text-slate-500">Empresa activa</dt>
              <dd className="font-semibold text-slate-950">{companyName}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Uso</dt>
              <dd className="text-slate-700">Identidad comercial del negocio</dd>
            </div>
          </dl>
        </div>
      </div>
    </Card>
  )
}

function MultisiteAdminPanel() {
  return (
    <Card className="overflow-hidden p-0">
      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="p-5">
          <StatusBadge label="Multisede" tone="info" />
          <h2 className="mt-3 text-xl font-semibold text-slate-950">
            Operacion multisede completa
          </h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Controla disponibilidad de servicios, profesionales, horarios, permisos de usuario
            y canales WhatsApp por sede.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link to="/admin/multisite">
              <Button>Abrir operacion multisede</Button>
            </Link>
          </div>
        </div>
        <div className="border-t border-[var(--color-border)] bg-slate-50 p-5 lg:border-l lg:border-t-0">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
            Cobertura
          </p>
          <ul className="mt-4 space-y-2 text-sm text-slate-700">
            <li>Servicios por sucursal</li>
            <li>Profesionales y horarios por sede</li>
            <li>Permisos y canales por sede</li>
          </ul>
        </div>
      </div>
    </Card>
  )
}

function LocationsAdminPanel() {
  return (
    <Card className="overflow-hidden p-0">
      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="p-5">
          <StatusBadge label="Multisede" tone="info" />
          <h2 className="mt-3 text-xl font-semibold text-slate-950">Sedes del negocio</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Define las sedes que se usaran en agenda, conversaciones y futuras reglas de
            profesionales por sucursal.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link to="/admin/locations">
              <Button>Abrir sedes</Button>
            </Link>
          </div>
        </div>
        <div className="border-t border-[var(--color-border)] bg-slate-50 p-5 lg:border-l lg:border-t-0">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
            Operacion
          </p>
          <p className="mt-4 text-2xl font-semibold text-slate-950">Agenda multisede</p>
          <p className="mt-2 text-sm leading-6 text-slate-700">
            Las citas pueden quedar asociadas a una sede real y no solo a una ubicacion en texto.
          </p>
        </div>
      </div>
    </Card>
  )
}

function WhatsAppAdminPanel({ status }: { status: string }) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="p-5">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge label="Canal" tone="info" />
            <StatusBadge label={toWhatsAppChannelLabel(status)} tone={getWhatsAppChannelTone(status)} />
          </div>
          <h2 className="mt-3 text-xl font-semibold text-slate-950">Canal de WhatsApp</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Controla el estado del canal, reconexion y pruebas de envio segun la configuracion
            vigente del negocio.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link to="/admin/whatsapp-channel">
              <Button>Abrir canal</Button>
            </Link>
          </div>
        </div>
        <div className="border-t border-[var(--color-border)] bg-amber-50 p-5 lg:border-l lg:border-t-0">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-amber-700">
            Estado del canal
          </p>
          <p className="mt-4 text-2xl font-semibold text-slate-950">{toWhatsAppChannelLabel(status)}</p>
          <p className="mt-2 text-sm leading-6 text-slate-700">
            Prioriza una configuracion trazable, con controles de envio y detencion inmediata.
          </p>
        </div>
      </div>
    </Card>
  )
}

function UsersAdminPanel({ activeUsers, totalUsers }: { activeUsers: number; totalUsers: number }) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="p-5">
          <StatusBadge label="Accesos" tone="info" />
          <h2 className="mt-3 text-xl font-semibold text-slate-950">Usuarios y roles</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Revisa usuarios registrados, roles operativos y asignacion de responsables para
            conversaciones, citas y administracion del negocio.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link to="/admin/users">
              <Button>Abrir usuarios</Button>
            </Link>
          </div>
        </div>
        <div className="border-t border-[var(--color-border)] bg-slate-50 p-5 lg:border-l lg:border-t-0">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
            Resumen de usuarios
          </p>
          <p className="mt-4 text-2xl font-semibold text-slate-950">
            {activeUsers}/{totalUsers}
          </p>
          <p className="mt-2 text-sm leading-6 text-slate-700">
            Usuarios activos sobre el total registrado.
          </p>
        </div>
      </div>
    </Card>
  )
}

function SecurityAdminPanel({ sessionTimeoutMinutes }: { sessionTimeoutMinutes: number }) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="p-5">
          <StatusBadge label="Seguridad" tone="neutral" />
          <h2 className="mt-3 text-xl font-semibold text-slate-950">Seguridad</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Centraliza politicas, sesiones, auditoria y controles administrativos. La vista conserva
            una estructura compacta para mantener el panel contenido.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link to="/admin/security">
              <Button>Abrir seguridad</Button>
            </Link>
          </div>
        </div>
        <div className="border-t border-[var(--color-border)] bg-slate-50 p-5 lg:border-l lg:border-t-0">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Sesion</p>
          <p className="mt-4 text-2xl font-semibold text-slate-950">{sessionTimeoutMinutes} min</p>
          <p className="mt-2 text-sm leading-6 text-slate-700">
            Tiempo de espera configurado para sesiones administrativas.
          </p>
        </div>
      </div>
    </Card>
  )
}
