import type { RouteObject } from 'react-router-dom'
import { Navigate, createBrowserRouter, useParams } from 'react-router-dom'
import type { ReactNode } from 'react'
import {
  AdminAssignmentsPage,
  AdminContentPage,
  AdminLocationsPage,
  AdminProfessionalFormPage,
  AdminProfessionalsPage,
  AdminRoomFormPage,
  AdminRoomsPage,
  AdminSecurityPage,
  AdminServicesPage,
  AdminUserFormPage,
  AdminUsersPage,
  AdministrationPage,
  CompanySettingsPage,
  WhatsAppSimulatorPage,
  WhatsAppChannelPage,
} from '../modules/administration'
import {
  AppointmentDetailPage,
  AppointmentsPage,
  BookingCancellationPage,
  BookingConfirmationPage,
  BookingReschedulePage,
  CreatePublicBookingPage,
  CustomerBookingsPage,
  EditAppointmentPage,
  NewAppointmentFromConversationPage,
  NewAppointmentFromProspectPage,
  NewAppointmentFlowPage,
  RescheduleAppointmentPage,
} from '../modules/bookings'
import { DashboardPage } from '../modules/dashboard/pages/DashboardPage'
import { ForgotPasswordPage } from '../modules/auth/pages/ForgotPasswordPage'
import { ForgotPasswordSentPage } from '../modules/auth/pages/ForgotPasswordSentPage'
import { LoginPage } from '../modules/auth/pages/LoginPage'
import {
  ConversationsPage,
  NewConversationPage,
  NewTemplatePage,
  TemplatesPage,
} from '../modules/conversations'
import {
  EditLeadPage,
  LeadDetailPage,
  NewLeadFromConversationPage,
  NewLeadPage,
  ProspectsPage,
} from '../modules/leads'
import { NotificationsPage } from '../modules/notifications/pages/NotificationsPage'
import { ResetPasswordPage } from '../modules/auth/pages/ResetPasswordPage'
import { ChangePasswordPage } from '../modules/security/pages/ChangePasswordPage'
import { ProfilePage } from '../modules/security/pages/ProfilePage'
import { CatalogFormPage, CatalogPage } from '../modules/catalog'
import { BusinessAiPage } from '../modules/business-ai'
import { CompleteAgendaPage } from '../modules/agenda'
import { MultisiteOperationsPage } from '../modules/multisite'
import { ConfigurationPage } from '../modules/configuration'
import {
  AutomationRuleFormPage,
  AutomationRuleRunPage,
  AutomationRulesPage,
} from '../modules/rules'
import { ReportsPage } from '../modules/reports'
import { BeautyCenterLandingPage } from '../modules/centros/pages/BeautyCenterLandingPage'
import { CenterPublicPage } from '../modules/centros/pages/CenterPublicPage'
import { CenterPublicPageDinamica } from '../modules/centros/pages/CenterPublicPageDinamica_actualizada'
import { CenterWhatsAppRedirect } from '../modules/centros/pages/CenterWhatsAppRedirect'

import { PrivateRouteShell, PublicRouteShell } from './RouteShells'
import { RequireRole } from './RequireRole'
import { useShellSession } from '../lib/shellSession'

export const appRoutes: RouteObject[] = [
  { path: '/reservas/confirmar/:token', element: <BookingConfirmationPage /> },
  { path: '/reservas/reprogramar/:token', element: <BookingReschedulePage /> },
  { path: '/reservas/cancelar/:token', element: <BookingCancellationPage /> },

  { path: '/reservar', element: <CreatePublicBookingPage /> },
  { path: '/reservas/mis-reservas/:token', element: <CustomerBookingsPage /> },

  { path: '/centro-estetica-bella', element: <BeautyCenterLandingPage /> },
  { path: '/centros/:slug', element: <CenterPublicPage /> },
  { path: '/centros/:slug/whatsapp', element: <CenterWhatsAppRedirect /> },

  { path: '/', element: <LandingPageWrapper /> },

  {
    element: <PublicRouteShell />,
    children: [
      { path: 'login', element: <LoginPage /> },
      { path: 'forgot-password', element: <ForgotPasswordPage /> },
      { path: 'forgot-password/sent', element: <ForgotPasswordSentPage /> },
      { path: 'reset-password', element: <ResetPasswordPage /> },
    ],
  },
  {
    path: '/',
    element: <PrivateRouteShell />,
    children: [
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'notifications', element: <NotificationsPage /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'profile/change-password', element: <ChangePasswordPage /> },
      {
        path: 'conversations',
        element: (
          <RequirePermission permission="CONVERSATIONS_VIEW">
            <ConversationsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'conversations/new',
        element: (
          <RequirePermission permission="CONVERSATIONS_REPLY">
            <NewConversationPage />
          </RequirePermission>
        ),
      },
      {
        path: 'conversations/:conversationId',
        element: (
          <RequirePermission permission="CONVERSATIONS_VIEW">
            <ConversationsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'conversations/:conversationId/prospects/new',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <NewLeadFromConversationPage />
          </RequirePermission>
        ),
      },

      {
        path: 'conversations/:conversationId/appointments/new',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <NewAppointmentFromConversationPage />
          </RequirePermission>
        ),
      },
      {
        path: 'templates',
        element: (
          <RequirePermission permission="TEMPLATE_MANAGE">
            <TemplatesPage />
          </RequirePermission>
        ),
      },
      {
        path: 'templates/new',
        element: (
          <RequirePermission permission="TEMPLATE_MANAGE">
            <NewTemplatePage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <ProspectsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/new',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <NewLeadPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/:prospectId',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <LeadDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/:prospectId/edit',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <EditLeadPage />
          </RequirePermission>
        ),
      },
      {
        path: 'agenda',
        element: (
          <RequirePermission permission="AGENDA_VIEW">
            <CompleteAgendaPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <AppointmentsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/new',
        element: (
           <RequirePermission permission="BOOKINGS_CREATE">
            <NewAppointmentFlowPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/:appointmentId',
        element: (
          <RequirePermission permission="BOOKINGS_UPDATE">
            <AppointmentDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/:appointmentId/edit',
        element: (
          <RequirePermission permission="BOOKINGS_UPDATE">
            <EditAppointmentPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/:appointmentId/reschedule',
        element: (
          <RequirePermission permission="BOOKINGS_RESCHEDULE">
            <RescheduleAppointmentPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/:prospectId/appointments/new',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <NewAppointmentFromProspectPage />
          </RequirePermission>
        ),
      },

      {
        path: 'catalog',
        element: (
          <RequirePermission permission="CATALOG_VIEW">
            <CatalogPage />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/products/new',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <Navigate replace to="/catalog" />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/services/new',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <CatalogFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/products/:productId/edit',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <Navigate replace to="/catalog" />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/services/:serviceId/edit',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <CatalogFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRulesPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules/new',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRuleFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules/:ruleId/edit',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRuleFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules/:ruleId/test',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRuleRunPage />
          </RequirePermission>
        ),
      },
      {
        path: 'business-ai',
        element: (
          <RequirePermission permission="BUSINESS_AI_VIEW" fallback={
            <section className="mx-auto max-w-[1440px] px-4 py-6">
              <div className="mt-20 flex flex-col items-center gap-4 text-center">
                <h1 className="text-2xl font-semibold text-gray-800">Acceso denegado</h1>
                <p className="text-gray-500">No tienes permisos para acceder a esta sección.</p>
              </div>
            </section>
          }>
            <BusinessAiPage />
          </RequirePermission>
        ),
      },
      {
        path: 'reports',
        element: (
          <RequirePermission permission="REPORTS_VIEW">
            <ReportsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'configuration',
        element: (
          <RequireAnyPermission permissions={['WHATSAPP_CONFIG_VIEW', 'CALENDAR_CONFIG_VIEW']}>
            <ConfigurationPage />
          </RequireAnyPermission>
        ),
      },
      {
        path: 'admin',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="ADMIN_MANAGE">
              <AdministrationPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/company',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="LOCATIONS_MANAGE">
              <CompanySettingsPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/locations',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="LOCATIONS_MANAGE">
              <AdminLocationsPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/content',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="CONTENT_VIEW">
              <AdminContentPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/multisite',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="ADMIN_MANAGE">
              <MultisiteOperationsPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/users',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="USERS_MANAGE">
              <AdminUsersPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/users/new',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="USERS_MANAGE">
              <AdminUserFormPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/users/:userId/edit',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="USERS_MANAGE">
              <AdminUserFormPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/whatsapp-channel',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="WHATSAPP_CONFIG_MANAGE">
              <WhatsAppChannelPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/whatsapp-simulator',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="WHATSAPP_CONFIG_MANAGE">
              <WhatsAppSimulatorPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/security',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="SECURITY_AUDIT_VIEW">
              <AdminSecurityPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/professionals',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="PROFESSIONAL_VIEW">
              <AdminProfessionalsPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/professionals/new',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="PROFESSIONAL_MANAGE">
              <AdminProfessionalFormPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/professionals/:professionalId/edit',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="PROFESSIONAL_MANAGE">
              <AdminProfessionalFormPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/rooms',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="ROOM_VIEW">
              <AdminRoomsPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/rooms/new',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="ROOM_MANAGE">
              <AdminRoomFormPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/rooms/:roomId/edit',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="ROOM_MANAGE">
              <AdminRoomFormPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
      {
        path: 'admin/branches',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <AdminLocationsPage />
          </RequireRole>
        ),
      },
      {
        path: 'admin/services',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <AdminServicesPage />
          </RequireRole>
        ),
      },
      {
        path: 'admin/assignments',
        element: (
          <RequireRole allowedRoles={['OWNER', 'ADMIN']}>
            <RequirePermission permission="ASSIGNMENT_VIEW">
              <AdminAssignmentsPage />
            </RequirePermission>
          </RequireRole>
        ),
      },
    ],
  },
  { path: '*', element: <Navigate replace to="/login" /> },
]

export const router = createBrowserRouter(appRoutes)

// Componente para verificar permisos granulares
// eslint-disable-next-line react-refresh/only-export-components
function RequirePermission({ permission, children, fallback }: { permission: string; children: ReactNode; fallback?: ReactNode }) {
  const { user } = useShellSession()

  if (!user || !user.permissions?.includes(permission)) {
    return fallback ?? <Navigate replace to="/dashboard" />
  }

  return children
}

// Componente para verificar que el usuario tenga al menos uno de los permisos
// eslint-disable-next-line react-refresh/only-export-components
function RequireAnyPermission({
  permissions,
  children,
}: {
  permissions: string[]
  children: ReactNode
}) {
  const { user } = useShellSession()

  if (!user || !permissions.some((p) => user.permissions?.includes(p))) {
    return <Navigate replace to="/dashboard" />
  }

  return children
}

function LandingPageWrapper() {
  return <CenterPublicPageDinamica />
}
